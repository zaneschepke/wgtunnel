package com.zaneschepke.wireguardautotunnel.data.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GitHubRelease(
    @SerialName("tag_name") val tagName: String,
    val name: String?,
    val body: String?,
    val assets: List<Asset>,
)
