package com.zaneschepke.wireguardautotunnel.viewmodel

import androidx.lifecycle.viewModelScope
import com.wireguard.android.backend.WgQuickBackend
import com.wireguard.android.util.RootShell
import com.zaneschepke.logcatter.LogReader
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.WireGuardAutoTunnel
import com.zaneschepke.wireguardautotunnel.core.service.ServiceManager
import com.zaneschepke.wireguardautotunnel.core.tunnel.TunnelManager
import com.zaneschepke.wireguardautotunnel.di.AppShell
import com.zaneschepke.wireguardautotunnel.di.IoDispatcher
import com.zaneschepke.wireguardautotunnel.domain.entity.TunnelConf
import com.zaneschepke.wireguardautotunnel.domain.enums.BackendState
import com.zaneschepke.wireguardautotunnel.domain.repository.AppDataRepository
import com.zaneschepke.wireguardautotunnel.ui.common.snackbar.SnackbarController
import com.zaneschepke.wireguardautotunnel.ui.state.AppUiState
import com.zaneschepke.wireguardautotunnel.util.Constants
import com.zaneschepke.wireguardautotunnel.util.LocaleUtil
import com.zaneschepke.wireguardautotunnel.util.StringValue
import com.zaneschepke.wireguardautotunnel.util.extensions.withData
import com.zaneschepke.wireguardautotunnel.viewmodel.event.AppEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.withContext
import timber.log.Timber
import xyz.teamgravity.pin_lock_compose.PinManager
import javax.inject.Inject
import javax.inject.Provider

@HiltViewModel
class AppViewModel
@Inject
constructor(
	appDataRepository: AppDataRepository,
	@IoDispatcher private val ioDispatcher: CoroutineDispatcher,
	@AppShell private val rootShell: Provider<RootShell>,
	private val tunnelManager: TunnelManager,
	private val serviceManager: ServiceManager,
	private val logReader: LogReader,
) : BaseViewModel(appDataRepository) {

	private val _isAppReady = MutableStateFlow(false)
	val isAppReady = _isAppReady.asStateFlow()

	private val _configurationChange = MutableStateFlow(false)
	val configurationChange = _configurationChange.asStateFlow()

	val uiState =
		combine(
			appDataRepository.settings.flow,
			appDataRepository.tunnels.flow,
			appDataRepository.appState.flow,
			tunnelManager.activeTunnels,
			serviceManager.autoTunnelActive,
		) { settings, tunnels, generalState, activeTunnels, autoTunnel ->
			AppUiState(
				settings,
				tunnels,
				activeTunnels,
				generalState,
				autoTunnel,
			)
		}.stateIn(
			viewModelScope + ioDispatcher,
			SharingStarted.Companion.WhileSubscribed(Constants.SUBSCRIPTION_TIMEOUT),
			AppUiState(),
		)

	init {
		viewModelScope.launch {
			initPin()
			handleKillSwitchChange()
			initServices()
			launch {
				initTunnels()
			}
			appReadyCheck()
		}
	}

	fun handleEvent(event: AppEvent) = viewModelScope.launch {
		when (event) {
			AppEvent.ToggleLocalLogging -> {
				val enabled = uiState.value.generalState.isLocalLogsEnabled
				appDataRepository.appState.setLocalLogsEnabled(!enabled)
				if (!enabled) logReader.start() else logReader.stop()
			}
		}
	}

	private suspend fun appReadyCheck() {
		val tunnelCount = appDataRepository.tunnels.count()
		uiState.first { it.tunnels.count() == tunnelCount }
		_isAppReady.emit(true)
	}

	private suspend fun initTunnels() {
		tunnels.withData { tunnels ->
			tunnels.filter { it.isActive }.forEach {
				tunnelManager.startTunnel(it)
			}
		}
	}

	private suspend fun initPin() {
		val isPinEnabled = appDataRepository.appState.isPinLockEnabled()
		if (isPinEnabled) PinManager.initialize(WireGuardAutoTunnel.instance)
	}

	private suspend fun initServices() {
		withContext(ioDispatcher) {
			appSettings.withData {
				if (it.isAutoTunnelEnabled) serviceManager.startAutoTunnel(false)
			}
		}
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

	fun onToggleAlwaysOnVPN() = viewModelScope.launch {
		with(uiState.value.appSettings) {
			appDataRepository.settings.save(
				copy(
					isAlwaysOnVpnEnabled = !isAlwaysOnVpnEnabled,
				),
			)
		}
	}

	fun onLocaleChange(localeTag: String) = viewModelScope.launch {
		appDataRepository.appState.setLocale(localeTag)
		LocaleUtil.changeLocale(localeTag)
		_configurationChange.update {
			true
		}
	}

	fun onToggleRestartAtBoot() = viewModelScope.launch {
		with(uiState.value.appSettings) {
			appDataRepository.settings.save(
				copy(
					isRestoreOnBootEnabled = !isRestoreOnBootEnabled,
				),
			)
		}
	}

	fun onToggleVpnKillSwitch(enabled: Boolean) = viewModelScope.launch {
		with(uiState.value.appSettings) {
			appDataRepository.settings.save(
				copy(
					isVpnKillSwitchEnabled = enabled,
					isLanOnKillSwitchEnabled = if (enabled) isLanOnKillSwitchEnabled else false,
				),
			)
		}
		handleKillSwitchChange()
	}

	private suspend fun handleKillSwitchChange() {
		withContext(ioDispatcher) {
			appSettings.withData {
				if (!it.isVpnKillSwitchEnabled) return@withData tunnelManager.setBackendState(BackendState.SERVICE_ACTIVE, emptyList())
				Timber.d("Starting kill switch")
				val allowedIps = if (it.isLanOnKillSwitchEnabled) TunnelConf.LAN_BYPASS_ALLOWED_IPS else emptyList()
				tunnelManager.setBackendState(BackendState.KILL_SWITCH_ACTIVE, allowedIps)
			}
		}
	}

	fun onToggleLanOnKillSwitch(enabled: Boolean) = viewModelScope.launch(ioDispatcher) {
		appDataRepository.settings.save(
			uiState.value.appSettings.copy(
				isLanOnKillSwitchEnabled = enabled,
			),
		)
		handleKillSwitchChange()
	}

	fun onToggleShortcutsEnabled() = viewModelScope.launch {
		with(uiState.value.appSettings) {
			appDataRepository.settings.save(
				copy(
					isShortcutsEnabled = !isShortcutsEnabled,
				),
			)
		}
	}

	private fun saveKernelMode(enabled: Boolean) = viewModelScope.launch {
		with(uiState.value.appSettings) {
			appDataRepository.settings.save(
				this.copy(
					isKernelEnabled = enabled,
				),
			)
		}
	}

	fun onToggleKernelMode() = viewModelScope.launch {
		with(uiState.value.appSettings) {
			if (!isKernelEnabled) {
				requestRoot().onSuccess {
					if (!isKernelSupported()) {
						return@onSuccess SnackbarController.showMessage(
							StringValue.StringResource(R.string.kernel_not_supported),
						)
					}
					tunnelManager.setBackendState(BackendState.INACTIVE, emptyList())
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
			runCatching {
				rootShell.get().start()
				SnackbarController.showMessage(StringValue.StringResource(R.string.root_accepted))
			}.onFailure {
				SnackbarController.showMessage(StringValue.StringResource(R.string.error_root_denied))
			}
		}
	}
}
