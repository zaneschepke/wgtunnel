package com.zaneschepke.wireguardautotunnel.data.repository

import com.zaneschepke.wireguardautotunnel.data.domain.GeneralState
import com.zaneschepke.wireguardautotunnel.ui.theme.Theme
import kotlinx.coroutines.flow.Flow

interface AppStateRepository {
	suspend fun isLocationDisclosureShown(): Boolean

	suspend fun setLocationDisclosureShown(shown: Boolean)

	suspend fun isPinLockEnabled(): Boolean

	suspend fun setPinLockEnabled(enabled: Boolean)

	suspend fun isBatteryOptimizationDisableShown(): Boolean

	suspend fun setBatteryOptimizationDisableShown(shown: Boolean)

	suspend fun isTunnelStatsExpanded(): Boolean

	suspend fun setTunnelStatsExpanded(expanded: Boolean)

	suspend fun setTheme(theme: Theme)

	suspend fun getTheme(): Theme

	suspend fun isLocalLogsEnabled(): Boolean

	suspend fun setLocalLogsEnabled(enabled: Boolean)

	suspend fun setLocale(localeTag: String)

	suspend fun getLocale(): String?

	val generalStateFlow: Flow<GeneralState>
}
