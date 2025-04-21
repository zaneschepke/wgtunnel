package com.zaneschepke.wireguardautotunnel.data.network

import com.zaneschepke.wireguardautotunnel.data.model.GitHubRelease

interface GitHubApi {
    suspend fun getLatestRelease(owner: String, repo: String): Result<GitHubRelease>
}
