package com.zaneschepke.wireguardautotunnel.data.repository

import android.content.Context
import com.zaneschepke.wireguardautotunnel.data.network.GitHubApi
import com.zaneschepke.wireguardautotunnel.di.IoDispatcher
import com.zaneschepke.wireguardautotunnel.domain.entity.AppUpdate
import com.zaneschepke.wireguardautotunnel.domain.repository.UpdateRepository
import com.zaneschepke.wireguardautotunnel.util.NumberUtils
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.contentLength
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readAvailable
import java.io.File
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

class GitHubUpdateRepository(
    private val gitHubApi: GitHubApi,
    private val httpClient: HttpClient,
    private val githubOwner: String,
    private val githubRepo: String,
    private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : UpdateRepository {
    override suspend fun checkForUpdate(currentVersion: String): Result<AppUpdate?> =
        withContext(ioDispatcher) {
            gitHubApi.getLatestRelease(githubOwner, githubRepo).map { release ->
                if (
                    NumberUtils.compareVersions(release.tagName.removePrefix("v"), currentVersion) >
                        0
                ) {
                    release.toAppUpdate()
                } else {
                    null
                }
            }
        }

    override suspend fun downloadApk(
        apkUrl: String,
        fileName: String,
        onProgress: (Float) -> Unit,
    ): Result<File> =
        withContext(ioDispatcher) {
            try {
                // clean up old files
                context.getExternalFilesDir(null)?.listFiles()?.forEach { file ->
                    if (file.extension == "apk") file.delete()
                }

                val response: HttpResponse = httpClient.get(apkUrl)

                val apkFile = File(context.getExternalFilesDir(null), fileName)

                val channel: ByteReadChannel = response.bodyAsChannel()
                val totalBytes: Long = response.contentLength() ?: -1L
                var bytesCopied = 0L

                apkFile.outputStream().use { output ->
                    val buffer = ByteArray(8 * 1024)

                    while (!channel.isClosedForRead) {
                        val bytesRead = channel.readAvailable(buffer)
                        if (bytesRead <= 0) break
                        output.write(buffer, 0, bytesRead)
                        bytesCopied += bytesRead

                        if (totalBytes > 0) {
                            val progress = bytesCopied.toFloat() / totalBytes
                            onProgress(progress.coerceIn(0f, 1f))
                        }
                    }
                }

                Result.success(apkFile)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
