package com.zaneschepke.wireguardautotunnel.data.repository

import com.zaneschepke.wireguardautotunnel.data.datastore.DataStoreManager
import com.zaneschepke.wireguardautotunnel.data.domain.GeneralState
import com.zaneschepke.wireguardautotunnel.module.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import timber.log.Timber

class DataStoreAppStateRepository(
	private val dataStoreManager: DataStoreManager,
	@IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) :
	AppStateRepository {
	override suspend fun isLocationDisclosureShown(): Boolean {
		return withContext(ioDispatcher) {
			dataStoreManager.getFromStore(DataStoreManager.LOCATION_DISCLOSURE_SHOWN)
				?: GeneralState.LOCATION_DISCLOSURE_SHOWN_DEFAULT
		}
	}

	override suspend fun setLocationDisclosureShown(shown: Boolean) {
		withContext(ioDispatcher) { dataStoreManager.saveToDataStore(DataStoreManager.LOCATION_DISCLOSURE_SHOWN, shown) }
	}

	override suspend fun isPinLockEnabled(): Boolean {
		return withContext(ioDispatcher) {
			dataStoreManager.getFromStore(DataStoreManager.IS_PIN_LOCK_ENABLED)
				?: GeneralState.PIN_LOCK_ENABLED_DEFAULT
		}
	}

	override suspend fun setPinLockEnabled(enabled: Boolean) {
		withContext(ioDispatcher) { dataStoreManager.saveToDataStore(DataStoreManager.IS_PIN_LOCK_ENABLED, enabled) }
	}

	override suspend fun isBatteryOptimizationDisableShown(): Boolean {
		return withContext(ioDispatcher) {
			dataStoreManager.getFromStore(DataStoreManager.BATTERY_OPTIMIZE_DISABLE_SHOWN)
				?: GeneralState.BATTERY_OPTIMIZATION_DISABLE_SHOWN_DEFAULT
		}
	}

	override suspend fun setBatteryOptimizationDisableShown(shown: Boolean) {
		withContext(ioDispatcher) { dataStoreManager.saveToDataStore(DataStoreManager.BATTERY_OPTIMIZE_DISABLE_SHOWN, shown) }
	}

	override suspend fun getCurrentSsid(): String? {
		return withContext(ioDispatcher) { dataStoreManager.getFromStore(DataStoreManager.CURRENT_SSID) }
	}

	override suspend fun setCurrentSsid(ssid: String) {
		withContext(ioDispatcher) { dataStoreManager.saveToDataStore(DataStoreManager.CURRENT_SSID, ssid) }
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
