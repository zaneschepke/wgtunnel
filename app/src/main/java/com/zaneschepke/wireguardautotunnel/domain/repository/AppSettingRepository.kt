package com.zaneschepke.wireguardautotunnel.domain.repository

import com.zaneschepke.wireguardautotunnel.domain.model.AppSettings
import kotlinx.coroutines.flow.Flow

interface AppSettingRepository {
    suspend fun save(appSettings: AppSettings)

    val flow: Flow<AppSettings>

    suspend fun get(): AppSettings
}
