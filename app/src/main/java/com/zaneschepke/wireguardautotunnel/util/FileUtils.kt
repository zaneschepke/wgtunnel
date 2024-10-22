package com.zaneschepke.wireguardautotunnel.util

import android.content.Context
import com.zaneschepke.wireguardautotunnel.data.domain.TunnelConfig
import com.zaneschepke.wireguardautotunnel.util.extensions.TunnelConfigs
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class FileUtils(
	private val context: Context,
	private val ioDispatcher: CoroutineDispatcher,
) {

	suspend fun createWgFiles(tunnels: TunnelConfigs): List<File> {
		return withContext(ioDispatcher) {
			tunnels.map { config ->
				val file = File(context.cacheDir, "${config.name}-wg.conf")
				file.outputStream().use {
					it.write(config.wgQuick.toByteArray())
				}
				file
			}
		}
	}

	suspend fun createAmFiles(tunnels: TunnelConfigs): List<File> {
		return withContext(ioDispatcher) {
			tunnels.filter { it.amQuick != TunnelConfig.AM_QUICK_DEFAULT }.map { config ->
				val file = File(context.cacheDir, "${config.name}-am.conf")
				file.outputStream().use {
					it.write(config.amQuick.toByteArray())
				}
				file
			}
		}
	}

	suspend fun zipAll(zipFile: File, files: List<File>) {
		withContext(ioDispatcher) {
			ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zos ->
				files.forEach { file ->
					val zipFileName = (
						file.parentFile?.let { parent ->
							file.absolutePath.removePrefix(parent.absolutePath)
						} ?: file.absolutePath
						).removePrefix("/")
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
	}

	suspend fun createNewShareFile(name: String): File {
		return withContext(ioDispatcher) {
			val sharePath = File(context.filesDir, "external_files")
			if (sharePath.exists()) sharePath.delete()
			sharePath.mkdir()
			val file = File("${sharePath.path}/$name")
			if (file.exists()) file.delete()
			file.createNewFile()
			file
		}
	}
}
