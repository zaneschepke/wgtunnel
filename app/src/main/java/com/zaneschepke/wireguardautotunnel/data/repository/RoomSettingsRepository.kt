package com.zaneschepke.wireguardautotunnel.data.repository

import com.zaneschepke.wireguardautotunnel.data.dao.SettingsDao
import com.zaneschepke.wireguardautotunnel.domain.repository.AppSettingRepository
import com.zaneschepke.wireguardautotunnel.data.model.Settings
import com.zaneschepke.wireguardautotunnel.di.IoDispatcher
import com.zaneschepke.wireguardautotunnel.domain.entity.AppSettings
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class RoomSettingsRepository(
	private val settingsDoa: SettingsDao,
	@IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : AppSettingRepository {

	override suspend fun save(appSettings: AppSettings) {
		withContext(ioDispatcher) {
			settingsDoa.save(Settings.from(appSettings))
		}
	}

	override val flow = settingsDoa.getSettingsFlow().flowOn(ioDispatcher).map { it.toAppSettings() }

	override suspend fun get(): AppSettings {
		return withContext(ioDispatcher) {
			(settingsDoa.getAll().firstOrNull() ?: Settings()).toAppSettings()
		}
	}
}
