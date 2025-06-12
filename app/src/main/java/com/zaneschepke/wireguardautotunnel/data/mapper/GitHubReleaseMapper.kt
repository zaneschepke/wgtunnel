package com.zaneschepke.wireguardautotunnel.data.mapper

import com.zaneschepke.wireguardautotunnel.data.entity.GitHubRelease
import com.zaneschepke.wireguardautotunnel.domain.model.AppUpdate
import kotlin.collections.firstOrNull

object GitHubReleaseMapper {
    fun toAppUpdate(gitHubRelease: GitHubRelease): AppUpdate {
        with(gitHubRelease) {
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
}
