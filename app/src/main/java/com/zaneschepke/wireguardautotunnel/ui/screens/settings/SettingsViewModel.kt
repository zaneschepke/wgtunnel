package com.zaneschepke.wireguardautotunnel.ui.screens.settings

import android.app.Application
import android.content.Context
import android.location.LocationManager
import androidx.core.location.LocationManagerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wireguard.android.util.RootShell
import com.zaneschepke.wireguardautotunnel.data.datastore.DataStoreManager
import com.zaneschepke.wireguardautotunnel.data.model.Settings
import com.zaneschepke.wireguardautotunnel.data.repository.SettingsRepository
import com.zaneschepke.wireguardautotunnel.data.repository.TunnelConfigRepository
import com.zaneschepke.wireguardautotunnel.service.foreground.ServiceManager
import com.zaneschepke.wireguardautotunnel.service.tunnel.VpnService
import com.zaneschepke.wireguardautotunnel.util.Constants
import com.zaneschepke.wireguardautotunnel.util.Event
import com.zaneschepke.wireguardautotunnel.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel
@Inject
constructor(
    private val application: Application,
    private val tunnelConfigRepository: TunnelConfigRepository,
    private val settingsRepository: SettingsRepository,
    private val dataStoreManager: DataStoreManager,
    private val rootShell: RootShell,
    private val vpnService: VpnService
) : ViewModel() {

    val uiState =
        combine(
                settingsRepository.getSettingsFlow(),
                tunnelConfigRepository.getTunnelConfigsFlow(),
                vpnService.vpnState,
                dataStoreManager.preferencesFlow,
            ) { settings, tunnels, tunnelState, preferences ->
                SettingsUiState(
                    settings,
                    tunnels,
                    tunnelState,
                    preferences?.get(DataStoreManager.LOCATION_DISCLOSURE_SHOWN) ?: false,
                    preferences?.get(DataStoreManager.BATTERY_OPTIMIZE_DISABLE_SHOWN) ?: false,
                )
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(Constants.SUBSCRIPTION_TIMEOUT),
                SettingsUiState(),
            )

    fun onSaveTrustedSSID(ssid: String): Result<Unit> {
        val trimmed = ssid.trim()
        return if (!uiState.value.settings.trustedNetworkSSIDs.contains(trimmed)) {
            uiState.value.settings.trustedNetworkSSIDs.add(trimmed)
            saveSettings(uiState.value.settings)
            Result.Success(Unit)
        } else {
            Result.Error(Event.Error.SsidConflict)
        }
    }

    fun setLocationDisclosureShown() =
        viewModelScope.launch {
            dataStoreManager.saveToDataStore(DataStoreManager.LOCATION_DISCLOSURE_SHOWN, true)
        }

    fun setBatteryOptimizeDisableShown() =
        viewModelScope.launch {
            dataStoreManager.saveToDataStore(DataStoreManager.BATTERY_OPTIMIZE_DISABLE_SHOWN, true)
        }

    fun onToggleTunnelOnMobileData() {
        saveSettings(
            uiState.value.settings.copy(
                isTunnelOnMobileDataEnabled = !uiState.value.settings.isTunnelOnMobileDataEnabled,
            ),
        )
    }

    fun onDeleteTrustedSSID(ssid: String) {
        saveSettings(
            uiState.value.settings.copy(
                trustedNetworkSSIDs =
                    (uiState.value.settings.trustedNetworkSSIDs - ssid).toMutableList(),
            ),
        )
    }

    private suspend fun getDefaultTunnelOrFirst(): String {
        return uiState.value.settings.defaultTunnel
            ?: tunnelConfigRepository.getAll().first().toString()
    }

    fun toggleAutoTunnel() =
        viewModelScope.launch {
            val isAutoTunnelEnabled = uiState.value.settings.isAutoTunnelEnabled
            var isAutoTunnelPaused = uiState.value.settings.isAutoTunnelPaused

            if (isAutoTunnelEnabled) {
                ServiceManager.stopWatcherService(application)
            } else {
                ServiceManager.startWatcherService(application)
                isAutoTunnelPaused = false
            }
            saveSettings(
                uiState.value.settings.copy(
                    isAutoTunnelEnabled = !isAutoTunnelEnabled,
                    isAutoTunnelPaused = isAutoTunnelPaused,
                    defaultTunnel = getDefaultTunnelOrFirst(),
                ),
            )
        }

    fun onToggleAlwaysOnVPN() =
        viewModelScope.launch {
            val updatedSettings =
                uiState.value.settings.copy(
                    isAlwaysOnVpnEnabled = !uiState.value.settings.isAlwaysOnVpnEnabled,
                    defaultTunnel = getDefaultTunnelOrFirst(),
                )
            saveSettings(updatedSettings)
        }

    private fun saveSettings(settings: Settings) =
        viewModelScope.launch { settingsRepository.save(settings) }

    fun onToggleTunnelOnEthernet() {
        saveSettings(
            uiState.value.settings.copy(
                isTunnelOnEthernetEnabled = !uiState.value.settings.isTunnelOnEthernetEnabled,
            ),
        )
    }

    fun isLocationEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return LocationManagerCompat.isLocationEnabled(locationManager)
    }

    fun onToggleShortcutsEnabled() {
        saveSettings(
            uiState.value.settings.copy(
                isShortcutsEnabled = !uiState.value.settings.isShortcutsEnabled,
            ),
        )
    }

    fun onToggleBatterySaver() {
        saveSettings(
            uiState.value.settings.copy(
                isBatterySaverEnabled = !uiState.value.settings.isBatterySaverEnabled,
            ),
        )
    }

    private fun saveKernelMode(on: Boolean) {
        saveSettings(
            uiState.value.settings.copy(
                isKernelEnabled = on,
            ),
        )
    }

    fun onToggleTunnelOnWifi() {
        saveSettings(
            uiState.value.settings.copy(
                isTunnelOnWifiEnabled = !uiState.value.settings.isTunnelOnWifiEnabled,
            ),
        )
    }

    fun onToggleKernelMode(): Result<Unit> {
        if (!uiState.value.settings.isKernelEnabled) {
            try {
                rootShell.start()
                Timber.i("Root shell accepted!")
                saveKernelMode(on = true)
            } catch (e: RootShell.RootShellException) {
                Timber.e(e)
                saveKernelMode(on = false)
                return Result.Error(Event.Error.RootDenied)
            }
        } else {
            saveKernelMode(on = false)
        }
        return Result.Success(Unit)
    }

    fun onToggleRestartOnPing() = viewModelScope.launch {
        saveSettings(
            uiState.value.settings.copy(
                isPingEnabled = !uiState.value.settings.isPingEnabled,
            ),
        )
    }
}
