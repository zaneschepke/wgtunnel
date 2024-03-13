package com.zaneschepke.wireguardautotunnel.util

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.MediaStore.MediaColumns
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.time.Instant
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object FileUtils {
    private const val ZIP_FILE_MIME_TYPE = "application/zip"

    private fun createDownloadsFileOutputStream(
        context: Context,
        fileName: String,
        mimeType: String = Constants.ALLOWED_FILE_TYPES
    ): OutputStream? {
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

    fun saveFileToDownloads(context: Context, content: String, fileName: String) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, Constants.TEXT_MIME_TYPE)
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                resolver.openOutputStream(uri).use { output ->
                    output?.write(content.toByteArray())
                }
            }
        } else {
            val target = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                fileName
            )
            FileOutputStream(target).use { output ->
                output.write(content.toByteArray())
            }
        }
    }

    fun saveFilesToZip(context: Context, files: List<File>) {
        val zipOutputStream =
            createDownloadsFileOutputStream(
                context,
                "wg-export_${Instant.now().epochSecond}.zip",
                ZIP_FILE_MIME_TYPE,
            )
        ZipOutputStream(zipOutputStream).use { zos ->
            files.forEach { file ->
                val entry = ZipEntry(file.name)
                zos.putNextEntry(entry)
                if (file.isFile) {
                    file.inputStream().use { fis -> fis.copyTo(zos) }
                }
            }
        }
    }
}
