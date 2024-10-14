package com.zaneschepke.wireguardautotunnel.util

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.MediaStore.MediaColumns
import com.zaneschepke.wireguardautotunnel.data.domain.TunnelConfig
import com.zaneschepke.wireguardautotunnel.util.extensions.TunnelConfigs
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.OutputStream
import java.time.Instant
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class FileUtils(
	private val context: Context,
	private val ioDispatcher: CoroutineDispatcher,
) {

	fun createWgFiles(tunnels: TunnelConfigs): List<File> {
		return tunnels.map { config ->
			val file = File(context.cacheDir, "${config.name}-wg.conf")
			file.outputStream().use {
				it.write(config.wgQuick.toByteArray())
			}
			file
		}
	}

	fun createAmFiles(tunnels: TunnelConfigs): List<File> {
		return tunnels.filter { it.amQuick != TunnelConfig.AM_QUICK_DEFAULT }.map { config ->
			val file = File(context.cacheDir, "${config.name}-am.conf")
			file.outputStream().use {
				it.write(config.amQuick.toByteArray())
			}
			file
		}
	}

	suspend fun saveFilesToZip(files: List<File>): Result<Unit> {
		return withContext(ioDispatcher) {
			try {
				val zipOutputStream =
					createDownloadsFileOutputStream(
						"wg-export_${Instant.now().epochSecond}.zip",
						Constants.ZIP_FILE_MIME_TYPE,
					)
				ZipOutputStream(zipOutputStream).use { zos ->
					files.forEach { file ->
						val entry = ZipEntry(file.name)
						zos.putNextEntry(entry)
						if (file.isFile) {
							file.inputStream().use { fis -> fis.copyTo(zos) }
						}
					}
					return@withContext Result.success(Unit)
				}
			} catch (e: Exception) {
				Timber.e(e)
				Result.failure(ConfigExportException)
			}
		}
	}

	// TODO issue with android 9
	private fun createDownloadsFileOutputStream(fileName: String, mimeType: String = Constants.ALL_FILE_TYPES): OutputStream? {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
			val resolver = context.contentResolver
			val contentValues =
				ContentValues().apply {
					put(MediaColumns.DISPLAY_NAME, fileName)
					put(MediaColumns.MIME_TYPE, mimeType)
					put(MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
				}
			val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
			if (uri != null) {
				return resolver.openOutputStream(uri)
			}
		} else {
			val target =
				File(
					Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
					fileName,
				)
			return target.outputStream()
		}
		return null
	}
}
