package com.zaneschepke.wireguardautotunnel.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wireguard.android.backend.WgQuickBackend
import com.wireguard.android.util.RootShell
import com.wireguard.config.Config
import com.zaneschepke.logcatter.LogReader
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.WireGuardAutoTunnel
import com.zaneschepke.wireguardautotunnel.data.domain.TunnelConfig
import com.zaneschepke.wireguardautotunnel.data.repository.AppDataRepository
import com.zaneschepke.wireguardautotunnel.module.AppShell
import com.zaneschepke.wireguardautotunnel.module.IoDispatcher
import com.zaneschepke.wireguardautotunnel.service.foreground.ServiceManager
import com.zaneschepke.wireguardautotunnel.service.tunnel.BackendState
import com.zaneschepke.wireguardautotunnel.service.tunnel.TunnelService
import com.zaneschepke.wireguardautotunnel.service.tunnel.TunnelState
import com.zaneschepke.wireguardautotunnel.ui.common.snackbar.SnackbarController
import com.zaneschepke.wireguardautotunnel.ui.screens.tunneloptions.config.model.InterfaceProxy
import com.zaneschepke.wireguardautotunnel.ui.screens.tunneloptions.config.model.PeerProxy
import com.zaneschepke.wireguardautotunnel.util.Constants
import com.zaneschepke.wireguardautotunnel.util.LocaleUtil
import com.zaneschepke.wireguardautotunnel.util.StringValue
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
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
import timber.log.Timber
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

	private val _popBackStack = MutableSharedFlow<Boolean>()
	val popBackStack = _popBackStack.asSharedFlow()

	private val _isAppReady = MutableStateFlow(false)
	val isAppReady = _isAppReady.asStateFlow()

	private val _configurationChange = MutableStateFlow(false)
	val configurationChange = _configurationChange.asStateFlow()

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

	init {
		viewModelScope.launch {
			initPin()
			initServices()
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
		if (tunnelService.get().getState() == TunnelState.UP) tunnelService.get().startActiveTunnelJobs()
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

	private suspend fun initServices() {
		withContext(ioDispatcher) {
			val settings = appDataRepository.settings.getSettings()
			handleVpnKillSwitchChange(settings.isVpnKillSwitchEnabled)
			if (settings.isAutoTunnelEnabled) serviceManager.startAutoTunnel(false)
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
		appDataRepository.appState.setLocale(localeTag)
		LocaleUtil.changeLocale(localeTag)
		_configurationChange.update {
			true
		}
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

	fun onToggleVpnKillSwitch(enabled: Boolean) = viewModelScope.launch {
		with(uiState.value.settings) {
			appDataRepository.settings.save(
				copy(
					isVpnKillSwitchEnabled = enabled,
					isLanOnKillSwitchEnabled = if (enabled) isLanOnKillSwitchEnabled else false,
				),
			)
		}
		handleVpnKillSwitchChange(enabled)
	}

	private suspend fun handleVpnKillSwitchChange(enabled: Boolean) {
		withContext(ioDispatcher) {
			if (enabled) {
				Timber.d("Starting kill switch")
				val allowedIps = if (appDataRepository.settings.getSettings().isLanOnKillSwitchEnabled) {
					TunnelConfig.IPV4_PUBLIC_NETWORKS
				} else {
					emptySet()
				}
				tunnelService.get().setBackendState(BackendState.KILL_SWITCH_ACTIVE, allowedIps)
			} else {
				Timber.d("Sending shutdown of kill switch")
				tunnelService.get().setBackendState(BackendState.SERVICE_ACTIVE, emptySet())
			}
		}
	}

	fun onToggleLanOnKillSwitch(enabled: Boolean) = viewModelScope.launch(ioDispatcher) {
		appDataRepository.settings.save(
			uiState.value.settings.copy(
				isLanOnKillSwitchEnabled = enabled,
			),
		)
		val allowedIps = if (enabled) TunnelConfig.IPV4_PUBLIC_NETWORKS else emptySet()
		Timber.d("Setting allowedIps $allowedIps")
		tunnelService.get().setBackendState(BackendState.KILL_SWITCH_ACTIVE, allowedIps)
	}

	fun onToggleShortcutsEnabled() = viewModelScope.launch {
		with(uiState.value.settings) {
			appDataRepository.settings.save(
				copy(
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
					tunnelService.get().setBackendState(BackendState.INACTIVE, emptyList())
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

	suspend fun requestRoot(): Result<Unit> {
		return withContext(ioDispatcher) {
			kotlin.runCatching {
				rootShell.get().start()
				SnackbarController.showMessage(StringValue.StringResource(R.string.root_accepted))
			}.onFailure {
				SnackbarController.showMessage(StringValue.StringResource(R.string.error_root_denied))
			}
		}
	}

	fun saveConfigChanges(config: TunnelConfig, peers: List<PeerProxy>? = null, `interface`: InterfaceProxy? = null) = viewModelScope.launch(
		ioDispatcher,
	) {
		runCatching {
			val amConfig = config.toAmConfig()
			val wgConfig = config.toWgConfig()
			rebuildConfigsAndSave(config, amConfig, wgConfig, peers, `interface`)
			_popBackStack.emit(true)
			SnackbarController.showMessage(StringValue.StringResource(R.string.config_changes_saved))
		}.onFailure {
			Timber.e(it)
			SnackbarController.showMessage(
				it.message?.let { message ->
					(StringValue.DynamicString(message))
				} ?: StringValue.StringResource(R.string.unknown_error),
			)
		}
	}

	fun cleanUpUninstalledApps(tunnelConfig: TunnelConfig, packages: List<String>) = viewModelScope.launch(ioDispatcher) {
		runCatching {
			val amConfig = tunnelConfig.toAmConfig()
			val wgConfig = tunnelConfig.toWgConfig()
			val proxy = InterfaceProxy.from(amConfig.`interface`)
			if (proxy.includedApplications.isEmpty() && proxy.excludedApplications.isEmpty()) return@launch
			if (proxy.includedApplications.retainAll(packages.toSet()) || proxy.excludedApplications.retainAll(packages.toSet())) {
				Timber.i("Removing split tunnel package for app that no longer exists on the device")
				rebuildConfigsAndSave(tunnelConfig, amConfig, wgConfig, `interface` = proxy)
			}
		}.onFailure {
			Timber.e(it)
		}
	}

	private suspend fun rebuildConfigsAndSave(
		config: TunnelConfig,
		amConfig: org.amnezia.awg.config.Config,
		wgConfig: Config,
		peers: List<PeerProxy>? = null,
		`interface`: InterfaceProxy? = null,
	) {
		appDataRepository.tunnels.save(
			config.copy(
				wgQuick = Config.Builder().apply {
					addPeers(peers?.map { it.toWgPeer() } ?: wgConfig.peers)
					setInterface(`interface`?.toWgInterface() ?: wgConfig.`interface`)
				}.build().toWgQuickString(true),
				amQuick = org.amnezia.awg.config.Config.Builder().apply {
					addPeers(peers?.map { it.toAmPeer() } ?: amConfig.peers)
					setInterface(`interface`?.toAmInterface() ?: amConfig.`interface`)
				}.build().toAwgQuickString(true),
			),
		)
	}
}
