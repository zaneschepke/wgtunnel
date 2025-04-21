package com.zaneschepke.wireguardautotunnel.data.model

import com.zaneschepke.wireguardautotunnel.domain.entity.AppUpdate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GitHubRelease(
    @SerialName("tag_name") val tagName: String,
    val name: String?,
    val body: String?,
    val assets: List<Asset>,
) {
    fun toAppUpdate(): AppUpdate {
        val apkAsset = assets.firstOrNull { it.name.endsWith(".apk") }
        return AppUpdate(
            version = tagName.removePrefix("v"),
            title = name ?: "Update $tagName",
            releaseNotes = body ?: "No release notes provided",
            apkUrl = apkAsset?.browserDownloadUrl,
            apkFileName = apkAsset?.name,
        )
    }
}
