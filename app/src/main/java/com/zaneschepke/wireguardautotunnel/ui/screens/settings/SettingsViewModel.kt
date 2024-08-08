package com.zaneschepke.wireguardautotunnel.ui.screens.settings

import android.content.Context
import android.location.LocationManager
import androidx.core.location.LocationManagerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wireguard.android.backend.WgQuickBackend
import com.wireguard.android.util.RootShell
import com.zaneschepke.wireguardautotunnel.data.domain.Settings
import com.zaneschepke.wireguardautotunnel.data.repository.AppDataRepository
import com.zaneschepke.wireguardautotunnel.module.IoDispatcher
import com.zaneschepke.wireguardautotunnel.service.foreground.ServiceManager
import com.zaneschepke.wireguardautotunnel.service.tunnel.TunnelService
import com.zaneschepke.wireguardautotunnel.util.Constants
import com.zaneschepke.wireguardautotunnel.util.FileUtils
import com.zaneschepke.wireguardautotunnel.util.WgTunnelExceptions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Provider

@HiltViewModel
class SettingsViewModel
@Inject
constructor(
	private val appDataRepository: AppDataRepository,
	private val serviceManager: ServiceManager,
	private val rootShell: Provider<RootShell>,
	private val fileUtils: FileUtils,
	@IoDispatcher private val ioDispatcher: CoroutineDispatcher,
	tunnelService: TunnelService,
) : ViewModel() {
	private val _kernelSupport = MutableStateFlow(false)
	val kernelSupport = _kernelSupport.asStateFlow()

	val uiState =
		combine(
			appDataRepository.settings.getSettingsFlow(),
			appDataRepository.tunnels.getTunnelConfigsFlow(),
			tunnelService.vpnState,
			appDataRepository.appState.generalStateFlow,
		) { settings, tunnels, tunnelState, generalState ->
			SettingsUiState(
				settings,
				tunnels,
				tunnelState,
				generalState.isLocationDisclosureShown,
				generalState.isBatteryOptimizationDisableShown,
				generalState.isPinLockEnabled,
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

	fun setLocationDisclosureShown() = viewModelScope.launch {
		appDataRepository.appState.setLocationDisclosureShown(true)
	}

	fun setBatteryOptimizeDisableShown() = viewModelScope.launch {
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

	suspend fun onExportTunnels(files: List<File>): Result<Unit> {
		return fileUtils.saveFilesToZip(files)
	}

	fun onToggleAutoTunnel(context: Context) = viewModelScope.launch {
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
	}

	fun onToggleAlwaysOnVPN() = viewModelScope.launch {
		saveSettings(
			uiState.value.settings.copy(
				isAlwaysOnVpnEnabled = !uiState.value.settings.isAlwaysOnVpnEnabled,
			),
		)
	}

	private fun saveSettings(settings: Settings) = viewModelScope.launch { appDataRepository.settings.save(settings) }

	fun onToggleTunnelOnEthernet() {
		saveSettings(
			uiState.value.settings.copy(
				isTunnelOnEthernetEnabled = !uiState.value.settings.isTunnelOnEthernetEnabled,
			),
		)
	}

	fun isLocationEnabled(context: Context): Boolean {
		val locationManager =
			context.getSystemService(
				Context.LOCATION_SERVICE,
			) as LocationManager
		return LocationManagerCompat.isLocationEnabled(locationManager)
	}

	fun onToggleShortcutsEnabled() {
		saveSettings(
			uiState.value.settings.copy(
				isShortcutsEnabled = !uiState.value.settings.isShortcutsEnabled,
			),
		)
	}

	private fun saveKernelMode(enabled: Boolean) {
		saveSettings(
			uiState.value.settings.copy(
				isKernelEnabled = enabled,
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
		if (uiState.value.settings.isKernelEnabled) {
			saveKernelMode(false)
		}
		saveAmneziaMode(!uiState.value.settings.isAmneziaEnabled)
	}

	private fun saveAmneziaMode(on: Boolean) {
		saveSettings(
			uiState.value.settings.copy(
				isAmneziaEnabled = on,
			),
		)
	}

	suspend fun onToggleKernelMode(): Result<Unit> {
		return withContext(ioDispatcher) {
			if (!uiState.value.settings.isKernelEnabled) {
				requestRoot().onSuccess {
					saveSettings(
						uiState.value.settings.copy(
							isKernelEnabled = true,
							isAmneziaEnabled = false,
						),
					)
				}.onFailure {
					Timber.e(it)
					saveKernelMode(enabled = false)
					return@withContext Result.failure(WgTunnelExceptions.RootDenied())
				}
			} else {
				saveKernelMode(enabled = false)
			}
			Result.success(Unit)
		}
	}

	fun onToggleRestartOnPing() = viewModelScope.launch {
		saveSettings(
			uiState.value.settings.copy(
				isPingEnabled = !uiState.value.settings.isPingEnabled,
			),
		)
	}

	fun checkKernelSupport() = viewModelScope.launch {
		val kernelSupport =
			withContext(ioDispatcher) {
				WgQuickBackend.hasKernelSupport()
			}
		_kernelSupport.update {
			kernelSupport
		}
	}

	fun onToggleRestartAtBoot() = viewModelScope.launch {
		saveSettings(
			uiState.value.settings.copy(
				isRestoreOnBootEnabled = !uiState.value.settings.isRestoreOnBootEnabled,
			),
		)
	}

	fun requestRoot(): Result<Unit> {
		return kotlin.runCatching {
			rootShell.get().start()
			Timber.i("Root shell accepted!")
		}.onFailure {
			return Result.failure(WgTunnelExceptions.RootDenied())
		}
	}
}
