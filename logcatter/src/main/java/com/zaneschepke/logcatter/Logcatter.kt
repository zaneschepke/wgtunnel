package com.zaneschepke.logcatter

import android.content.Context
import com.zaneschepke.logcatter.model.LogLevel
import com.zaneschepke.logcatter.model.LogMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.BufferedReader
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

object LogcatHelper {

    private const val MAX_FILE_SIZE = 2097152L // 2MB
    private const val MAX_FOLDER_SIZE = 10485760L // 10MB
    private val findKeyRegex = """[A-Za-z0-9+/]{42}[AEIMQUYcgkosw480]=""".toRegex()
    private val findIpv6AddressRegex =
        """(([0-9a-fA-F]{1,4}:){7,7}[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,7}:|([0-9a-fA-F]{1,4}:){1,6}:[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,5}(:[0-9a-fA-F]{1,4}){1,2}|([0-9a-fA-F]{1,4}:){1,4}(:[0-9a-fA-F]{1,4}){1,3}|([0-9a-fA-F]{1,4}:){1,3}(:[0-9a-fA-F]{1,4}){1,4}|([0-9a-fA-F]{1,4}:){1,2}(:[0-9a-fA-F]{1,4}){1,5}|[0-9a-fA-F]{1,4}:((:[0-9a-fA-F]{1,4}){1,6})|:((:[0-9a-fA-F]{1,4}){1,7}|:)|fe80:(:[0-9a-fA-F]{0,4}){0,4}%[0-9a-zA-Z]{1,}|::(ffff(:0{1,4}){0,1}:){0,1}((25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])\.){3,3}(25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])|([0-9a-fA-F]{1,4}:){1,4}:((25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])\.){3,3}(25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9]))""".toRegex()
    private val findIpv4AddressRegex = """((25[0-5]|(2[0-4]|1\d|[1-9]|)\d)\.?\b){4}""".toRegex()
    private val findTunnelNameRegex = """(?<=tunnel ).*?(?= UP| DOWN)""".toRegex()
    private const val CHORE = "Choreographer"

    private object LogcatHelperInit {
        var maxFileSize: Long = MAX_FILE_SIZE
        var maxFolderSize: Long = MAX_FOLDER_SIZE
        var pID: Int = 0
        var publicAppDirectory = ""
        var logcatPath = ""
    }

    fun init(
        maxFileSize: Long = MAX_FILE_SIZE,
        maxFolderSize: Long = MAX_FOLDER_SIZE,
        context: Context
    ): LocalLogCollector {
        if (maxFileSize > maxFolderSize) {
            throw IllegalStateException("maxFileSize must be less than maxFolderSize")
        }
        synchronized(LogcatHelperInit) {
            LogcatHelperInit.maxFileSize = maxFileSize
            LogcatHelperInit.maxFolderSize = maxFolderSize
            LogcatHelperInit.pID = android.os.Process.myPid()
            context.getExternalFilesDir(null)?.let {
                LogcatHelperInit.publicAppDirectory = it.absolutePath
                LogcatHelperInit.logcatPath =
                    LogcatHelperInit.publicAppDirectory + File.separator + "logs"
                val logDirectory = File(LogcatHelperInit.logcatPath)
                if (!logDirectory.exists()) {
                    logDirectory.mkdir()
                }
            }
            return Logcat
        }
    }

    internal object Logcat : LocalLogCollector {

        private var logcatReader: LogcatReader? = null

        override fun start(onLogMessage: ((message: LogMessage) -> Unit)?) {
            logcatReader ?: run {
                logcatReader = LogcatReader(
                    LogcatHelperInit.pID.toString(),
                    LogcatHelperInit.logcatPath,
                    onLogMessage,
                )
            }
            logcatReader?.let { logReader ->
                if (!logReader.isAlive) logReader.start()
            }
        }

        override fun stop() {
            logcatReader?.stopLogs()
            logcatReader = null
        }

        private fun mergeLogsApi26(sourceDir: String, outputFile: File) {
            val outputFilePath = Paths.get(outputFile.absolutePath)
            val logcatPath = Paths.get(sourceDir)

            Files.list(logcatPath).use {
                it.sorted { o1, o2 ->
                    Files.getLastModifiedTime(o1).compareTo(Files.getLastModifiedTime(o2))
                }
                    .flatMap(Files::lines).use { lines ->
                        lines.forEach { line ->
                            Files.write(
                                outputFilePath,
                                (line + System.lineSeparator()).toByteArray(),
                                StandardOpenOption.CREATE,
                                StandardOpenOption.APPEND,
                            )
                        }
                    }
            }
        }

        override suspend fun getLogFile(): Result<File> {
            stop()
            return withContext(Dispatchers.IO) {
                try {
                    val outputDir =
                        File(LogcatHelperInit.publicAppDirectory + File.separator + "output")
                    val outputFile = File(outputDir.absolutePath + File.separator + "logs.txt")

                    if (!outputDir.exists()) outputDir.mkdir()
                    if (outputFile.exists()) outputFile.delete()

                    mergeLogsApi26(LogcatHelperInit.logcatPath, outputFile)
                    Result.success(outputFile)
                } catch (e: Exception) {
                    Result.failure(e)
                } finally {
                    start()
                }
            }
        }

        private val _bufferedLogs = MutableSharedFlow<LogMessage>(
            replay = 10_000,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )

        override val bufferedLogs: Flow<LogMessage> = _bufferedLogs.asSharedFlow()

        private class LogcatReader(
            pID: String,
            private val logcatPath: String,
            private val callback: ((input: LogMessage) -> Unit)?,
        ) : Thread() {
            private var logcatProc: Process? = null
            private var reader: BufferedReader? = null
            private var mRunning = true
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

            fun stopLogs() {
                mRunning = false
            }

            fun clear() {
                Runtime.getRuntime().exec(clearLogCommand)
            }

            private fun obfuscator(log: String): String {
                return findKeyRegex.replace(log, "<crypto-key>").let { first ->
                    findIpv6AddressRegex.replace(first, "<ipv6-address>").let { second ->
                        findTunnelNameRegex.replace(second, "<tunnel>")
                    }
                }.let { last -> findIpv4AddressRegex.replace(last, "<ipv4-address>") }
            }

            override fun run() {
                if (outputStream == null) return
                try {
                    clear()
                    logcatProc = Runtime.getRuntime().exec(command)
                    reader = BufferedReader(InputStreamReader(logcatProc!!.inputStream), 1024)
                    var line: String? = null

                    while (mRunning && run {
                            line = reader!!.readLine()
                            line
                        } != null
                    ) {
                        if (!mRunning) {
                            break
                        }
                        if (line!!.isEmpty()) {
                            continue
                        }

                        if (outputStream!!.channel.size() >= LogcatHelperInit.maxFileSize) {
                            outputStream!!.close()
                            outputStream = FileOutputStream(createLogFile(logcatPath))
                        }
                        if (getFolderSize(logcatPath) >= LogcatHelperInit.maxFolderSize) {
                            deleteOldestFile(logcatPath)
                        }
                        line?.let { text ->
                            val obfuscated = obfuscator(text)
                            outputStream!!.write((obfuscated + System.lineSeparator()).toByteArray())
                            try {
                                val logMessage = LogMessage.from(obfuscated)
                                when (logMessage.level) {
                                    LogLevel.VERBOSE -> Unit
                                    else -> {
                                        if (!logMessage.tag.contains(CHORE)) {
                                            _bufferedLogs.tryEmit(logMessage)
                                        }
                                    }
                                }
                                callback?.let {
                                    it(logMessage)
                                }
                            } catch (e: Exception) {
                                Timber.e(e)
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

            private fun deleteOldestFile(path: String) {
                val directory = File(path)
                if (directory.isDirectory) {
                    directory.listFiles()?.toMutableList()?.run {
                        this.sortBy { it.lastModified() }
                        this.first().delete()
                    }
                }
            }
        }
    }
}
