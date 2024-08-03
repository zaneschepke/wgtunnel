package com.zaneschepke.wireguardautotunnel.data.repository

import com.zaneschepke.wireguardautotunnel.data.SettingsDao
import com.zaneschepke.wireguardautotunnel.data.domain.Settings
import com.zaneschepke.wireguardautotunnel.module.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class RoomSettingsRepository(private val settingsDoa: SettingsDao, @IoDispatcher private val ioDispatcher: CoroutineDispatcher) : SettingsRepository {
	override suspend fun save(settings: Settings) {
		withContext(ioDispatcher) {
			settingsDoa.save(settings)
		}
	}

	override fun getSettingsFlow(): Flow<Settings> {
		return settingsDoa.getSettingsFlow()
	}

	override suspend fun getSettings(): Settings {
		return withContext(ioDispatcher) {
			settingsDoa.getAll().firstOrNull() ?: Settings()
		}
	}

	override suspend fun getAll(): List<Settings> {
		return withContext(ioDispatcher) { settingsDoa.getAll() }
	}
}
