package com.zaneschepke.wireguardautotunnel.data.network

import com.zaneschepke.wireguardautotunnel.data.entity.GitHubRelease

interface GitHubApi {
    suspend fun getLatestRelease(owner: String, repo: String): Result<GitHubRelease>

    suspend fun getNightlyRelease(owner: String, repo: String): Result<GitHubRelease>
}
