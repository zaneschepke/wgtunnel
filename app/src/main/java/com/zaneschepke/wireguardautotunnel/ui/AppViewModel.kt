package com.zaneschepke.wireguardautotunnel.ui

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wireguard.android.backend.WgQuickBackend
import com.wireguard.android.util.RootShell
import com.zaneschepke.logcatter.LogReader
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.WireGuardAutoTunnel
import com.zaneschepke.wireguardautotunnel.data.repository.AppDataRepository
import com.zaneschepke.wireguardautotunnel.module.AppShell
import com.zaneschepke.wireguardautotunnel.module.IoDispatcher
import com.zaneschepke.wireguardautotunnel.service.foreground.ServiceManager
import com.zaneschepke.wireguardautotunnel.service.tunnel.TunnelService
import com.zaneschepke.wireguardautotunnel.service.tunnel.TunnelState
import com.zaneschepke.wireguardautotunnel.ui.common.snackbar.SnackbarController
import com.zaneschepke.wireguardautotunnel.util.Constants
import com.zaneschepke.wireguardautotunnel.util.LocaleUtil
import com.zaneschepke.wireguardautotunnel.util.LocaleUtil.OPTION_PHONE_LANGUAGE
import com.zaneschepke.wireguardautotunnel.util.StringValue
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.withContext
import xyz.teamgravity.pin_lock_compose.PinManager
import javax.inject.Inject
import javax.inject.Provider

@HiltViewModel
class AppViewModel
@Inject
constructor(
	private val appDataRepository: AppDataRepository,
	private val tunnelService: Provider<TunnelService>,
	@IoDispatcher private val ioDispatcher: CoroutineDispatcher,
	@AppShell private val rootShell: Provider<RootShell>,
	private val serviceManager: ServiceManager,
	private val logReader: LogReader,
) : ViewModel() {

	val uiState =
		combine(
			appDataRepository.settings.getSettingsFlow(),
			appDataRepository.tunnels.getTunnelConfigsFlow(),
			tunnelService.get().vpnState,
			appDataRepository.appState.generalStateFlow,
			serviceManager.autoTunnelActive,
		) { settings, tunnels, tunnelState, generalState, autoTunnel ->
			AppUiState(
				settings,
				tunnels,
				tunnelState,
				generalState,
				autoTunnel,
			)
		}.stateIn(
			viewModelScope + ioDispatcher,
			SharingStarted.WhileSubscribed(Constants.SUBSCRIPTION_TIMEOUT),
			AppUiState(),
		)

	private val _isAppReady = MutableStateFlow(false)
	val isAppReady = _isAppReady.asStateFlow()

	private val _configurationChange = MutableStateFlow(false)
	val configurationChange = _configurationChange.asStateFlow()

	init {
		viewModelScope.launch {
			initPin()
			initAutoTunnel()
			initTunnel()
			appReadyCheck()
		}
	}

	private suspend fun appReadyCheck() {
		val tunnelCount = appDataRepository.tunnels.count()
		uiState.takeWhile { it.tunnels.size != tunnelCount }.onCompletion {
			_isAppReady.emit(true)
		}.collect()
	}

	private suspend fun initTunnel() {
		if (tunnelService.get().getState() == TunnelState.UP) tunnelService.get().startStatsJob()
		val activeTunnels = appDataRepository.tunnels.getActive()
		if (activeTunnels.isNotEmpty() &&
			tunnelService.get().getState() == TunnelState.DOWN
		) {
			tunnelService.get().startTunnel(activeTunnels.first())
		}
	}

	private suspend fun initPin() {
		val isPinEnabled = appDataRepository.appState.isPinLockEnabled()
		if (isPinEnabled) PinManager.initialize(WireGuardAutoTunnel.instance)
	}

	private suspend fun initAutoTunnel() {
		val settings = appDataRepository.settings.getSettings()
		if (settings.isAutoTunnelEnabled) serviceManager.startAutoTunnel(false)
	}

	fun onPinLockDisabled() = viewModelScope.launch(ioDispatcher) {
		PinManager.clearPin()
		appDataRepository.appState.setPinLockEnabled(false)
	}

	fun onPinLockEnabled() = viewModelScope.launch {
		appDataRepository.appState.setPinLockEnabled(true)
	}

	fun setLocationDisclosureShown() = viewModelScope.launch {
		appDataRepository.appState.setLocationDisclosureShown(true)
	}

	fun onToggleLocalLogging() = viewModelScope.launch(ioDispatcher) {
		with(uiState.value.generalState) {
			val toggledOn = !isLocalLogsEnabled
			appDataRepository.appState.setLocalLogsEnabled(toggledOn)
			if (!toggledOn) onLoggerStop()
			_configurationChange.update {
				true
			}
		}
	}

	private suspend fun onLoggerStop() {
		logReader.stop()
		logReader.deleteAndClearLogs()
	}

	fun onToggleAlwaysOnVPN() = viewModelScope.launch {
		with(uiState.value.settings) {
			appDataRepository.settings.save(
				copy(
					isAlwaysOnVpnEnabled = !isAlwaysOnVpnEnabled,
				),
			)
		}
	}

	fun onLocaleChange(localeTag: String) = viewModelScope.launch {
		val locale = LocaleUtil.getLocaleFromPrefCode(localeTag)
		val storageLocale = if (localeTag == OPTION_PHONE_LANGUAGE) OPTION_PHONE_LANGUAGE else locale
		appDataRepository.appState.setLocale(storageLocale)
		val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(locale)
		AppCompatDelegate.setApplicationLocales(appLocale)
	}

	fun onToggleRestartAtBoot() = viewModelScope.launch {
		with(uiState.value.settings) {
			appDataRepository.settings.save(
				copy(
					isRestoreOnBootEnabled = !isRestoreOnBootEnabled,
				),
			)
		}
	}

	fun onToggleShortcutsEnabled() = viewModelScope.launch {
		with(uiState.value.settings) {
			appDataRepository.settings.save(
				this.copy(
					isShortcutsEnabled = !isShortcutsEnabled,
				),
			)
		}
	}

	private fun saveKernelMode(enabled: Boolean) = viewModelScope.launch {
		with(uiState.value.settings) {
			appDataRepository.settings.save(
				this.copy(
					isKernelEnabled = enabled,
				),
			)
		}
	}

	fun onToggleKernelMode() = viewModelScope.launch {
		with(uiState.value.settings) {
			if (!isKernelEnabled) {
				requestRoot().onSuccess {
					if (!isKernelSupported()) return@onSuccess SnackbarController.showMessage(StringValue.StringResource(R.string.kernel_not_supported))
					appDataRepository.settings.save(
						copy(
							isKernelEnabled = true,
							isAmneziaEnabled = false,
						),
					)
				}
			} else {
				saveKernelMode(enabled = false)
			}
		}
	}

	private suspend fun isKernelSupported(): Boolean {
		return withContext(ioDispatcher) {
			WgQuickBackend.hasKernelSupport()
		}
	}

	private suspend fun requestRoot(): Result<Unit> {
		return withContext(ioDispatcher) {
			kotlin.runCatching {
				rootShell.get().start()
				SnackbarController.showMessage(StringValue.StringResource(R.string.root_accepted))
			}.onFailure {
				SnackbarController.showMessage(StringValue.StringResource(R.string.error_root_denied))
			}
		}
	}
}
