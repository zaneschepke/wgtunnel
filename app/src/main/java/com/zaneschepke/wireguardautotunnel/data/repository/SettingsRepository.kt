package com.zaneschepke.wireguardautotunnel.data.repository

import com.zaneschepke.wireguardautotunnel.data.domain.Settings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
	suspend fun save(settings: Settings)

	fun getSettingsFlow(): Flow<Settings>

	suspend fun getSettings(): Settings

	suspend fun getAll(): List<Settings>
}
