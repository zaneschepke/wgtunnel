package com.zaneschepke.wireguardautotunnel.data.repository

import com.zaneschepke.wireguardautotunnel.data.DataStoreManager
import com.zaneschepke.wireguardautotunnel.data.model.GeneralState
import com.zaneschepke.wireguardautotunnel.domain.entity.AppState
import com.zaneschepke.wireguardautotunnel.domain.repository.AppStateRepository
import com.zaneschepke.wireguardautotunnel.ui.theme.Theme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber

class DataStoreAppStateRepository(
	private val dataStoreManager: DataStoreManager,
) :
	AppStateRepository {
	override suspend fun isLocationDisclosureShown(): Boolean {
		return dataStoreManager.getFromStore(DataStoreManager.locationDisclosureShown)
			?: GeneralState.LOCATION_DISCLOSURE_SHOWN_DEFAULT
	}

	override suspend fun setLocationDisclosureShown(shown: Boolean) {
		dataStoreManager.saveToDataStore(DataStoreManager.locationDisclosureShown, shown)
	}

	override suspend fun isPinLockEnabled(): Boolean {
		return dataStoreManager.getFromStore(DataStoreManager.pinLockEnabled)
			?: GeneralState.PIN_LOCK_ENABLED_DEFAULT
	}

	override suspend fun setPinLockEnabled(enabled: Boolean) {
		dataStoreManager.saveToDataStore(DataStoreManager.pinLockEnabled, enabled)
	}

	override suspend fun isBatteryOptimizationDisableShown(): Boolean {
		return dataStoreManager.getFromStore(DataStoreManager.batteryDisableShown)
			?: GeneralState.BATTERY_OPTIMIZATION_DISABLE_SHOWN_DEFAULT
	}

	override suspend fun setBatteryOptimizationDisableShown(shown: Boolean) {
		dataStoreManager.saveToDataStore(DataStoreManager.batteryDisableShown, shown)
	}

	override suspend fun isTunnelStatsExpanded(): Boolean {
		return dataStoreManager.getFromStore(DataStoreManager.tunnelStatsExpanded)
			?: GeneralState.IS_TUNNEL_STATS_EXPANDED
	}

	override suspend fun setTunnelStatsExpanded(expanded: Boolean) {
		dataStoreManager.saveToDataStore(DataStoreManager.tunnelStatsExpanded, expanded)
	}

	override suspend fun setTheme(theme: Theme) {
		dataStoreManager.saveToDataStore(DataStoreManager.theme, theme.name)
	}

	override suspend fun getTheme(): Theme {
		return dataStoreManager.getFromStore(DataStoreManager.theme)?.let {
			try {
				Theme.valueOf(it)
			} catch (_: IllegalArgumentException) {
				Theme.AUTOMATIC
			}
		} ?: Theme.AUTOMATIC
	}

	override suspend fun isLocalLogsEnabled(): Boolean {
		return dataStoreManager.getFromStore(DataStoreManager.isLocalLogsEnabled) ?: GeneralState.IS_LOGS_ENABLED_DEFAULT
	}

	override suspend fun setLocalLogsEnabled(enabled: Boolean) {
		dataStoreManager.saveToDataStore(DataStoreManager.isLocalLogsEnabled, enabled)
	}

	override suspend fun setLocale(localeTag: String) {
		dataStoreManager.saveToDataStore(DataStoreManager.locale, localeTag)
	}

	override suspend fun getLocale(): String? {
		return dataStoreManager.getFromStore(DataStoreManager.locale)
	}

	override suspend fun setIsRemoteControlEnabled(enabled: Boolean) {
		dataStoreManager.saveToDataStore(DataStoreManager.isRemoteControlEnabled, enabled)
	}

	override suspend fun isRemoteControlEnabled(): Boolean {
		return dataStoreManager.getFromStore(DataStoreManager.isRemoteControlEnabled) ?: GeneralState.IS_REMOTE_CONTROL_ENABLED
	}

	override suspend fun setRemoteKey(key: String) {
		dataStoreManager.saveToDataStore(DataStoreManager.remoteKey, key)
	}

	override suspend fun getRemoteKey(): String? {
		return dataStoreManager.getFromStore(DataStoreManager.remoteKey)
	}

	override val flow: Flow<AppState> =
		dataStoreManager.preferencesFlow.map { prefs ->
			prefs?.let { pref ->
				try {
					GeneralState(
						isLocationDisclosureShown =
						pref[DataStoreManager.locationDisclosureShown]
							?: GeneralState.LOCATION_DISCLOSURE_SHOWN_DEFAULT,
						isBatteryOptimizationDisableShown =
						pref[DataStoreManager.batteryDisableShown]
							?: GeneralState.BATTERY_OPTIMIZATION_DISABLE_SHOWN_DEFAULT,
						isPinLockEnabled =
						pref[DataStoreManager.pinLockEnabled]
							?: GeneralState.PIN_LOCK_ENABLED_DEFAULT,
						isTunnelStatsExpanded = pref[DataStoreManager.tunnelStatsExpanded] ?: GeneralState.IS_TUNNEL_STATS_EXPANDED,
						isLocalLogsEnabled = pref[DataStoreManager.isLocalLogsEnabled] ?: GeneralState.IS_LOGS_ENABLED_DEFAULT,
						isRemoteControlEnabled = pref[DataStoreManager.isRemoteControlEnabled] ?: GeneralState.IS_REMOTE_CONTROL_ENABLED,
						remoteKey = pref[DataStoreManager.remoteKey],
						locale = pref[DataStoreManager.locale],
						theme = getTheme(),
					)
				} catch (e: IllegalArgumentException) {
					Timber.e(e)
					GeneralState()
				}
			} ?: GeneralState()
		}.map { it.toAppState() }
}
