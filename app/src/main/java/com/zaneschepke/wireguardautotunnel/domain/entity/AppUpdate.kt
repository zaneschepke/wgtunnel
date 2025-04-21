package com.zaneschepke.wireguardautotunnel.domain.entity

data class AppUpdate(
    val version: String,
    val title: String,
    val releaseNotes: String,
    val apkUrl: String?,
    val apkFileName: String?,
)
