package com.zaneschepke.wireguardautotunnel.ui.screens.settings

import android.content.Context
import android.location.LocationManager
import androidx.core.location.LocationManagerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wireguard.android.util.RootShell
import com.zaneschepke.wireguardautotunnel.WireGuardAutoTunnel
import com.zaneschepke.wireguardautotunnel.data.domain.Settings
import com.zaneschepke.wireguardautotunnel.data.repository.AppDataRepository
import com.zaneschepke.wireguardautotunnel.service.foreground.ServiceManager
import com.zaneschepke.wireguardautotunnel.service.tunnel.VpnService
import com.zaneschepke.wireguardautotunnel.util.Constants
import com.zaneschepke.wireguardautotunnel.util.WgTunnelExceptions
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
    private val appDataRepository: AppDataRepository,
    private val serviceManager: ServiceManager,
    private val rootShell: RootShell,
    vpnService: VpnService
) : ViewModel() {

    val uiState =
        combine(
            appDataRepository.settings.getSettingsFlow(),
            appDataRepository.tunnels.getTunnelConfigsFlow(),
            vpnService.vpnState,
            appDataRepository.appState.generalStateFlow,
        ) { settings, tunnels, tunnelState, generalState ->
            SettingsUiState(
                settings,
                tunnels,
                tunnelState,
                generalState.locationDisclosureShown,
                generalState.batteryOptimizationDisableShown,
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
            Result.success(Unit)
        } else {
            Result.failure(WgTunnelExceptions.SsidConflict())
        }
    }

    fun setLocationDisclosureShown() =
        viewModelScope.launch {
            appDataRepository.appState.setLocationDisclosureShown(true)
        }

    fun setBatteryOptimizeDisableShown() =
        viewModelScope.launch {
            appDataRepository.appState.setBatteryOptimizationDisableShown(true)
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

    fun onToggleAutoTunnel(context: Context) =
        viewModelScope.launch {
            val isAutoTunnelEnabled = uiState.value.settings.isAutoTunnelEnabled
            var isAutoTunnelPaused = uiState.value.settings.isAutoTunnelPaused

            if (isAutoTunnelEnabled) {
                serviceManager.stopWatcherService(context)
            } else {
                serviceManager.startWatcherService(context)
                isAutoTunnelPaused = false
            }
            saveSettings(
                uiState.value.settings.copy(
                    isAutoTunnelEnabled = !isAutoTunnelEnabled,
                    isAutoTunnelPaused = isAutoTunnelPaused,
                ),
            )
            WireGuardAutoTunnel.requestAutoTunnelTileServiceUpdate()
        }

    fun onToggleAlwaysOnVPN() =
        viewModelScope.launch {
            saveSettings(
                uiState.value.settings.copy(
                    isAlwaysOnVpnEnabled = !uiState.value.settings.isAlwaysOnVpnEnabled,
                ),
            )
        }

    private fun saveSettings(settings: Settings) =
        viewModelScope.launch { appDataRepository.settings.save(settings) }

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

    fun onToggleAmnezia() = viewModelScope.launch {
        if(uiState.value.settings.isKernelEnabled) {
            saveKernelMode(false)
        }
        saveAmneziaMode(!uiState.value.settings.isAmneziaEnabled)
    }

    private fun saveAmneziaMode(on: Boolean) {
        saveSettings(
            uiState.value.settings.copy(
                isAmneziaEnabled = on
            )
        )
    }

    fun onToggleKernelMode(): Result<Unit> {
        if (!uiState.value.settings.isKernelEnabled) {
            try {
                rootShell.start()
                Timber.i("Root shell accepted!")
                saveSettings(
                    uiState.value.settings.copy(
                        isKernelEnabled = true,
                        isAmneziaEnabled = false,
                    ),
                )

            } catch (e: RootShell.RootShellException) {
                Timber.e(e)
                saveKernelMode(on = false)
                return Result.failure(WgTunnelExceptions.RootDenied())
            }
        } else {
            saveKernelMode(on = false)
        }
        return Result.success(Unit)
    }

    fun onToggleRestartOnPing() = viewModelScope.launch {
        saveSettings(
            uiState.value.settings.copy(
                isPingEnabled = !uiState.value.settings.isPingEnabled,
            ),
        )
    }
}
