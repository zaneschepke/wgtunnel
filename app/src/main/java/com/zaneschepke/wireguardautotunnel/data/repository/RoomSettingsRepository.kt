package com.zaneschepke.wireguardautotunnel.data.repository

import com.zaneschepke.wireguardautotunnel.data.dao.SettingsDao
import com.zaneschepke.wireguardautotunnel.data.entity.Settings
import com.zaneschepke.wireguardautotunnel.data.mapper.SettingsMapper
import com.zaneschepke.wireguardautotunnel.di.IoDispatcher
import com.zaneschepke.wireguardautotunnel.domain.model.AppSettings
import com.zaneschepke.wireguardautotunnel.domain.repository.AppSettingRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class RoomSettingsRepository(
    private val settingsDoa: SettingsDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : AppSettingRepository {

    override suspend fun save(appSettings: AppSettings) {
        withContext(ioDispatcher) { settingsDoa.save(SettingsMapper.toSettings(appSettings)) }
    }

    override val flow =
        settingsDoa.getSettingsFlow().flowOn(ioDispatcher).map(SettingsMapper::toAppSettings)

    override suspend fun get(): AppSettings {
        return withContext(ioDispatcher) {
            SettingsMapper.toAppSettings(settingsDoa.getAll().firstOrNull() ?: Settings())
        }
    }
}
