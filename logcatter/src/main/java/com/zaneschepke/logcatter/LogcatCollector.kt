package com.zaneschepke.logcatter

import android.content.Context
import com.zaneschepke.logcatter.model.LogMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object LogcatCollector {

	private const val MAX_FILE_SIZE = 2097152L // 2MB
	private const val MAX_FOLDER_SIZE = 10485760L // 10MB

	private val findKeyRegex = """[A-Za-z0-9+/]{42}[AEIMQUYcgkosw480]=""".toRegex()
	private val findIpv6AddressRegex = """^([0-9A-Fa-f]{1,4}:){7}[0-9A-Fa-f]{1,4}${'$'}""".toRegex()
	private val findIpv4AddressRegex = """((25[0-5]|(2[0-4]|1\d|[1-9]|)\d)\.?\b){4}""".toRegex()
	private val findTunnelNameRegex = """(?<=tunnel ).*?(?= UP| DOWN)""".toRegex()

	private val ioDispatcher = Dispatchers.IO

	private object LogcatHelperInit {
		var maxFileSize: Long = MAX_FILE_SIZE
		var maxFolderSize: Long = MAX_FOLDER_SIZE
		var pID: Int = 0
		var publicAppDirectory = ""
		var logcatPath = ""
	}

	fun init(maxFileSize: Long = MAX_FILE_SIZE, maxFolderSize: Long = MAX_FOLDER_SIZE, context: Context): LogReader {
		if (maxFileSize > maxFolderSize) {
			throw IllegalStateException("maxFileSize must be less than maxFolderSize")
		}
		synchronized(LogcatHelperInit) {
			LogcatHelperInit.maxFileSize = maxFileSize
			LogcatHelperInit.maxFolderSize = maxFolderSize
			LogcatHelperInit.pID = android.os.Process.myPid()
			context.getExternalFilesDir(null)?.let {
				LogcatHelperInit.publicAppDirectory = it.absolutePath
				LogcatHelperInit.logcatPath = LogcatHelperInit.publicAppDirectory + File.separator + "logs"
				val logDirectory = File(LogcatHelperInit.logcatPath)
				if (!logDirectory.exists()) {
					logDirectory.mkdir()
				}
			}
			return Logcat
		}
	}

	internal object Logcat : LogReader {

		private var logcatReader: LogcatReader? = null

		private fun obfuscator(log: String): String {
			return findKeyRegex.replace(log, "<crypto-key>").let { first ->
				findIpv6AddressRegex.replace(first, "<ipv6-address>").let { second ->
					findTunnelNameRegex.replace(second, "<tunnel>")
				}
			}.let { last -> findIpv4AddressRegex.replace(last, "<ipv4-address>") }
		}

		override suspend fun start(onLogMessage: ((message: LogMessage) -> Unit)?) {
			logcatReader ?: run {
				logcatReader = LogcatReader(LogcatHelperInit.pID.toString(), LogcatHelperInit.logcatPath, onLogMessage)
			}
			logcatReader?.run()
		}

		override fun stop() {
			logcatReader?.stop()
			logcatReader = null
		}

		override fun zipLogFiles(path: String) {
			logcatReader?.pause()
			zipAll(path)
			logcatReader?.resume()
		}

		private fun zipAll(zipFilePath: String) {
			val sourceFile = File(LogcatHelperInit.logcatPath)
			val outputZipFile = File(zipFilePath)
			ZipOutputStream(BufferedOutputStream(FileOutputStream(outputZipFile))).use { zos ->
				sourceFile.walkTopDown().forEach { file ->
					val zipFileName = file.absolutePath.removePrefix(sourceFile.absolutePath).removePrefix("/")
					val entry = ZipEntry("$zipFileName${(if (file.isDirectory) "/" else "")}")
					zos.putNextEntry(entry)
					if (file.isFile) {
						file.inputStream().use {
							it.copyTo(zos)
						}
					}
				}
			}
		}

		@OptIn(ExperimentalCoroutinesApi::class)
		override suspend fun deleteAndClearLogs() {
			withContext(ioDispatcher) {
				logcatReader?.pause()
				_bufferedLogs.resetReplayCache()
				logcatReader?.deleteAllFiles()
				logcatReader?.resume()
			}
		}

		private val _bufferedLogs = MutableSharedFlow<LogMessage>(
			replay = 10_000,
			onBufferOverflow = BufferOverflow.DROP_OLDEST,
		)
		private val _liveLogs = MutableSharedFlow<LogMessage>(
			replay = 1,
			onBufferOverflow = BufferOverflow.DROP_OLDEST,
		)

		override val bufferedLogs: Flow<LogMessage> = _bufferedLogs.asSharedFlow()

		override val liveLogs: Flow<LogMessage> = _liveLogs.asSharedFlow()

		private class LogcatReader(
			pID: String,
			private val logcatPath: String,
			private val callback: ((input: LogMessage) -> Unit)?,
		) {
			private var logcatProc: Process? = null
			private var reader: BufferedReader? = null

			@get:Synchronized @set:Synchronized
			private var paused = false

			@get:Synchronized @set:Synchronized
			private var stopped = false
			private var command = ""
			private var clearLogCommand = ""
			private var outputStream: FileOutputStream? = null

			init {
				try {
					outputStream = FileOutputStream(createLogFile(logcatPath))
				} catch (e: FileNotFoundException) {
					Timber.e(e)
				}

				command = "logcat -v epoch | grep \"($pID)\""
				clearLogCommand = "logcat -c"
			}

			fun pause() {
				paused = true
			}
			fun stop() {
				stopped = true
			}

			fun resume() {
				paused = false
			}

			fun clear() {
				Runtime.getRuntime().exec(clearLogCommand)
			}

			suspend fun run() {
				withContext(ioDispatcher) {
					paused = false
					stopped = false
					if (outputStream == null) return@withContext
					try {
						clear()
						logcatProc = Runtime.getRuntime().exec(command)
						reader = BufferedReader(InputStreamReader(logcatProc!!.inputStream), 1024)
						var line: String? = null

						while (!stopped) {
							if (paused) continue
							line = reader?.readLine()
							if (line.isNullOrEmpty()) continue
							outputStream?.let {
								if (it.channel.size() >= LogcatHelperInit.maxFileSize) {
									it.close()
									outputStream = createNewLogFileStream()
								}
								if (getFolderSize(logcatPath) >= LogcatHelperInit.maxFolderSize) {
									deleteOldestFile()
								}
								line.let { text ->
									val obfuscated = obfuscator(text)
									it.write((obfuscated + System.lineSeparator()).toByteArray())
									try {
										val logMessage = LogMessage.from(text)
										_bufferedLogs.tryEmit(logMessage)
										_liveLogs.tryEmit(logMessage)
										callback?.let {
											it(logMessage)
										}
									} catch (e: Exception) {
										Timber.e(e)
									}
								}
							}
						}
					} catch (e: IOException) {
						Timber.e(e)
					} finally {
						logcatProc?.destroy()
						logcatProc = null

						try {
							reader?.close()
							outputStream?.close()
							reader = null
							outputStream = null
						} catch (e: IOException) {
							Timber.e(e)
						}
					}
				}
			}

			private fun getFolderSize(path: String): Long {
				File(path).run {
					var size = 0L
					if (this.isDirectory && this.listFiles() != null) {
						for (file in this.listFiles()!!) {
							size += getFolderSize(file.absolutePath)
						}
					} else {
						size = this.length()
					}
					return size
				}
			}

			private fun createLogFile(dir: String): File {
				return File(dir, "logcat_" + System.currentTimeMillis() + ".txt")
			}

			fun deleteOldestFile() {
				val directory = File(logcatPath)
				if (directory.isDirectory) {
					directory.listFiles()?.toMutableList()?.run {
						this.sortBy { it.lastModified() }
						this.first().delete()
					}
				}
			}

			private fun createNewLogFileStream(): FileOutputStream {
				return FileOutputStream(createLogFile(logcatPath))
			}

			fun deleteAllFiles() {
				val directory = File(logcatPath)
				directory.listFiles()?.toMutableList()?.run {
					this.forEach { it.delete() }
				}
				outputStream = createNewLogFileStream()
			}
		}
	}
}
