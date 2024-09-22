package com.zaneschepke.wireguardautotunnel.data.repository

import com.zaneschepke.wireguardautotunnel.data.datastore.DataStoreManager
import com.zaneschepke.wireguardautotunnel.data.domain.GeneralState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber

class DataStoreAppStateRepository(
	private val dataStoreManager: DataStoreManager,
) :
	AppStateRepository {
	override suspend fun isLocationDisclosureShown(): Boolean {
		return dataStoreManager.getFromStore(DataStoreManager.LOCATION_DISCLOSURE_SHOWN)
			?: GeneralState.LOCATION_DISCLOSURE_SHOWN_DEFAULT
	}

	override suspend fun setLocationDisclosureShown(shown: Boolean) {
		dataStoreManager.saveToDataStore(DataStoreManager.LOCATION_DISCLOSURE_SHOWN, shown)
	}

	override suspend fun isPinLockEnabled(): Boolean {
		return dataStoreManager.getFromStore(DataStoreManager.IS_PIN_LOCK_ENABLED)
			?: GeneralState.PIN_LOCK_ENABLED_DEFAULT
	}

	override suspend fun setPinLockEnabled(enabled: Boolean) {
		dataStoreManager.saveToDataStore(DataStoreManager.IS_PIN_LOCK_ENABLED, enabled)
	}

	override suspend fun isBatteryOptimizationDisableShown(): Boolean {
		return dataStoreManager.getFromStore(DataStoreManager.BATTERY_OPTIMIZE_DISABLE_SHOWN)
			?: GeneralState.BATTERY_OPTIMIZATION_DISABLE_SHOWN_DEFAULT
	}

	override suspend fun setBatteryOptimizationDisableShown(shown: Boolean) {
		dataStoreManager.saveToDataStore(DataStoreManager.BATTERY_OPTIMIZE_DISABLE_SHOWN, shown)
	}

	override suspend fun getCurrentSsid(): String? {
		return dataStoreManager.getFromStore(DataStoreManager.CURRENT_SSID)
	}

	override suspend fun setCurrentSsid(ssid: String) {
		dataStoreManager.saveToDataStore(DataStoreManager.CURRENT_SSID, ssid)
	}

	override val generalStateFlow: Flow<GeneralState> =
		dataStoreManager.preferencesFlow.map { prefs ->
			prefs?.let { pref ->
				try {
					GeneralState(
						isLocationDisclosureShown =
						pref[DataStoreManager.LOCATION_DISCLOSURE_SHOWN]
							?: GeneralState.LOCATION_DISCLOSURE_SHOWN_DEFAULT,
						isBatteryOptimizationDisableShown =
						pref[DataStoreManager.BATTERY_OPTIMIZE_DISABLE_SHOWN]
							?: GeneralState.BATTERY_OPTIMIZATION_DISABLE_SHOWN_DEFAULT,
						isPinLockEnabled =
						pref[DataStoreManager.IS_PIN_LOCK_ENABLED]
							?: GeneralState.PIN_LOCK_ENABLED_DEFAULT,
					)
				} catch (e: IllegalArgumentException) {
					Timber.e(e)
					GeneralState()
				}
			} ?: GeneralState()
		}
}
