package com.zaneschepke.wireguardautotunnel.util

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.data.model.TunnelConfig
import com.zaneschepke.wireguardautotunnel.domain.entity.TunnelConf
import com.zaneschepke.wireguardautotunnel.util.extensions.getInputStreamFromUri
import com.zaneschepke.wireguardautotunnel.util.extensions.launchShareFile
import com.zaneschepke.wireguardautotunnel.util.extensions.toWgQuickString
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.amnezia.awg.config.Config
import timber.log.Timber

class FileUtils(private val context: Context, private val ioDispatcher: CoroutineDispatcher) {

    suspend fun createWgFiles(tunnels: List<TunnelConf>): List<File> =
        withContext(ioDispatcher) {
            tunnels.map { config ->
                val file = File(context.cacheDir, "${config.tunName}-wg.conf")
                file.outputStream().use { it.write(config.wgQuick.toByteArray()) }
                file
            }
        }

    suspend fun createAmFiles(tunnels: List<TunnelConf>): List<File> =
        withContext(ioDispatcher) {
            tunnels
                .filter { it.amQuick != TunnelConfig.AM_QUICK_DEFAULT }
                .map { config ->
                    val file = File(context.cacheDir, "${config.tunName}-am.conf")
                    file.outputStream().use { it.write(config.amQuick.toByteArray()) }
                    file
                }
        }

    suspend fun zipAll(zipFile: File, files: List<File>) =
        withContext(ioDispatcher) {
            ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zos ->
                files.forEach { file ->
                    val zipFileName =
                        (file.parentFile?.let { parent ->
                                file.absolutePath.removePrefix(parent.absolutePath)
                            } ?: file.absolutePath)
                            .removePrefix("/")
                    val entry = ZipEntry("$zipFileName${(if (file.isDirectory) "/" else "")}")
                    zos.putNextEntry(entry)
                    if (file.isFile) {
                        file.inputStream().use { it.copyTo(zos) }
                    }
                }
            }
        }

    suspend fun createNewShareFile(name: String): File =
        withContext(ioDispatcher) {
            val sharePath = File(context.filesDir, "external_files")
            if (sharePath.exists()) sharePath.delete()
            sharePath.mkdir()
            val file = File("${sharePath.path}/$name")
            if (file.exists()) file.delete()
            file.createNewFile()
            file
        }

    private fun getDisplayNameColumnIndex(cursor: Cursor): Int? {
        val columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (columnIndex == -1) return null
        return columnIndex
    }

    private fun getDisplayNameByCursor(cursor: Cursor): String? {
        val move = cursor.moveToFirst()
        if (!move) return null
        val index = getDisplayNameColumnIndex(cursor)
        if (index == null) return index
        return cursor.getString(index)
    }

    private fun isValidUriContentScheme(uri: Uri): Boolean {
        return uri.scheme == Constants.URI_CONTENT_SCHEME
    }

    private fun getFileName(uri: Uri): String {
        return getFileNameByCursor(uri) ?: NumberUtils.generateRandomTunnelName()
    }

    private fun getNameFromFileName(fileName: String): String {
        return fileName.substring(0, fileName.lastIndexOf('.'))
    }

    private fun getFileExtensionFromFileName(fileName: String): String? {
        return try {
            fileName.substring(fileName.lastIndexOf('.'))
        } catch (e: Exception) {
            Timber.e(e)
            null
        }
    }

    private fun getFileNameByCursor(uri: Uri): String? {
        return context.contentResolver.query(uri, null, null, null, null)?.use {
            getDisplayNameByCursor(it)
        }
    }

    suspend fun buildTunnelsFromUri(uri: Uri): List<TunnelConf> =
        withContext(ioDispatcher) {
            if (!isValidUriContentScheme(uri)) throw InvalidFileExtensionException
            val fileName = getFileName(uri)
            when (getFileExtensionFromFileName(fileName)) {
                Constants.CONF_FILE_EXTENSION -> {
                    context.getInputStreamFromUri(uri)?.use { inputStream ->
                        val name = getNameFromFileName(fileName)
                        val amConf = Config.parse(inputStream)
                        listOf(
                            TunnelConf(
                                tunName = name,
                                wgQuick = amConf.toWgQuickString(),
                                amQuick = amConf.toAwgQuickString(true),
                            )
                        )
                    } ?: throw FileReadException
                }
                Constants.ZIP_FILE_EXTENSION -> {
                    ZipInputStream(context.getInputStreamFromUri(uri)).use { zip ->
                        generateSequence { zip.nextEntry }
                            .filterNot {
                                it.isDirectory ||
                                    getFileExtensionFromFileName(it.name) !=
                                        Constants.CONF_FILE_EXTENSION
                            }
                            .map { entry ->
                                val name = getNameFromFileName(entry.name)
                                val amConf = Config.parse(zip.bufferedReader())
                                TunnelConf(
                                    tunName = name,
                                    wgQuick = amConf.toWgQuickString(),
                                    amQuick = amConf.toAwgQuickString(true),
                                )
                            }
                            .toList()
                    }
                }
                else -> throw InvalidFileExtensionException
            }
        }

    fun shareFile(shareFile: File) {
        val uri =
            FileProvider.getUriForFile(context, context.getString(R.string.provider), shareFile)
        context.launchShareFile(uri)
    }
}
