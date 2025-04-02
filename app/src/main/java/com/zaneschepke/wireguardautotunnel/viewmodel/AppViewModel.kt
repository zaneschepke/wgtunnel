package com.zaneschepke.wireguardautotunnel.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wireguard.android.backend.WgQuickBackend
import com.wireguard.android.util.RootShell
import com.zaneschepke.logcatter.LogReader
import com.zaneschepke.logcatter.model.LogMessage
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.WireGuardAutoTunnel
import com.zaneschepke.wireguardautotunnel.core.service.ServiceManager
import com.zaneschepke.wireguardautotunnel.core.shortcut.ShortcutManager
import com.zaneschepke.wireguardautotunnel.core.tunnel.TunnelManager
import com.zaneschepke.wireguardautotunnel.di.AppShell
import com.zaneschepke.wireguardautotunnel.di.IoDispatcher
import com.zaneschepke.wireguardautotunnel.di.MainDispatcher
import com.zaneschepke.wireguardautotunnel.domain.entity.AppSettings
import com.zaneschepke.wireguardautotunnel.domain.entity.TunnelConf
import com.zaneschepke.wireguardautotunnel.domain.enums.BackendState
import com.zaneschepke.wireguardautotunnel.domain.enums.ConfigType
import com.zaneschepke.wireguardautotunnel.domain.repository.AppDataRepository
import com.zaneschepke.wireguardautotunnel.ui.common.snackbar.SnackbarController
import com.zaneschepke.wireguardautotunnel.ui.state.AppState
import com.zaneschepke.wireguardautotunnel.ui.state.AppUiState
import com.zaneschepke.wireguardautotunnel.ui.theme.Theme
import com.zaneschepke.wireguardautotunnel.util.Constants
import com.zaneschepke.wireguardautotunnel.util.FileUtils
import com.zaneschepke.wireguardautotunnel.util.InvalidFileExtensionException
import com.zaneschepke.wireguardautotunnel.util.LocaleUtil
import com.zaneschepke.wireguardautotunnel.util.StringValue
import com.zaneschepke.wireguardautotunnel.util.extensions.withFirstState
import com.zaneschepke.wireguardautotunnel.viewmodel.event.AppEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.amnezia.awg.config.Config
import timber.log.Timber
import xyz.teamgravity.pin_lock_compose.PinManager
import java.net.URL
import java.time.Instant
import javax.inject.Inject
import javax.inject.Provider

@HiltViewModel
class AppViewModel
@Inject
constructor(
	val appDataRepository: AppDataRepository,
	@IoDispatcher private val ioDispatcher: CoroutineDispatcher,
	@MainDispatcher private val mainDispatcher: CoroutineDispatcher,
	@AppShell private val rootShell: Provider<RootShell>,
	private val tunnelManager: TunnelManager,
	private val serviceManager: ServiceManager,
	private val logReader: LogReader,
	private val fileUtils: FileUtils,
	private val shortcutManager: ShortcutManager,
) : ViewModel() {

	private val tunnelMutex = Mutex()
	private val settingsMutex = Mutex()
	private val loggerMutex = Mutex()
	private val tunControlMutex = Mutex()

	private val _appState = MutableStateFlow(AppState())
	val appState = _appState.asStateFlow()

	private val _logs = MutableStateFlow<List<LogMessage>>(emptyList())
	val logs: StateFlow<List<LogMessage>> = _logs.asStateFlow()

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
				isAppLoaded = true,
			)
		}.stateIn(
			viewModelScope + ioDispatcher,
			SharingStarted.Companion.WhileSubscribed(Constants.SUBSCRIPTION_TIMEOUT),
			AppUiState(),
		)

	init {
		viewModelScope.launch(ioDispatcher) {
			uiState.withFirstState { realState ->
				Timber.d("Real state: $realState")
				initPin(realState.generalState.isPinLockEnabled)
				handleKillSwitchChange(realState.appSettings)
				initServicesFromSavedState(realState)
				_appState.update { it.copy(isAppReady = true) }
			}
			uiState.filter { it.generalState.isLocalLogsEnabled }.first()
			collectLogs()
		}
	}

	fun handleEvent(event: AppEvent) = viewModelScope.launch(ioDispatcher) {
		uiState.withFirstState { state ->
			Timber.d("handleEvent: $event")
			when (event) {
				AppEvent.ToggleLocalLogging -> onToggleLocalLogging(state.generalState.isLocalLogsEnabled)
				is AppEvent.SetDebounceDelay -> onSetDebounceDelay(state.appSettings, event.delay)
				is AppEvent.CopyTunnel -> onCopyTunnel(event.tunnel, state.tunnels)
				is AppEvent.DeleteTunnel -> onDeleteTunnel(event.tunnel, state)
				is AppEvent.ImportTunnelFromClipboard -> onClipboardImport(event.text, state.tunnels)
				is AppEvent.ImportTunnelFromFile -> onImportTunnelFromFile(event.data, state.tunnels)
				is AppEvent.ImportTunnelFromUrl -> onImportTunnelFromUrl(event.url, state.tunnels)
				is AppEvent.ImportTunnelFromQrCode -> onImportTunnelFromQr(event.qrCode, state.tunnels)
				AppEvent.SetBatteryOptimizeDisableShown -> setBatteryOptimizeDisableShown()
				is AppEvent.StartTunnel -> onStartTunnel(event.tunnel)
				is AppEvent.StopTunnel -> onStopTunnel(event.tunnel)
				AppEvent.ToggleAutoTunnel -> onToggleAutoTunnel()
				AppEvent.ToggleTunnelStatsExpanded -> onToggleExpandTunnelStats(state.generalState.isTunnelStatsExpanded)
				AppEvent.ToggleAlwaysOn -> onToggleAlwaysOnVPN(state.appSettings)
				AppEvent.TogglePinLock -> onPinLockToggled(state.generalState.isPinLockEnabled)
				AppEvent.SetLocationDisclosureShown -> setLocationDisclosureShown()
				is AppEvent.SetLocale -> onLocaleChange(event.localeTag)
				AppEvent.ToggleRestartAtBoot -> onToggleRestartAtBoot(state.appSettings)
				AppEvent.ToggleVpnKillSwitch -> onToggleVpnKillSwitch(state.appSettings)
				AppEvent.ToggleLanOnKillSwitch -> onToggleLanOnKillSwitch(state.appSettings)
				AppEvent.ToggleAppShortcuts -> onToggleAppShortcuts(state.appSettings)
				AppEvent.ToggleKernelMode -> onToggleKernelMode(state.appSettings)
				is AppEvent.SetTheme -> onThemeChange(event.theme)
				is AppEvent.ToggleIpv4Preferred -> onToggleIpv4(event.tunnel)
				is AppEvent.TogglePrimaryTunnel -> onTogglePrimaryTunnel(event.tunnel)
				is AppEvent.SetTunnelPingCooldown -> onPingCoolDownChange(event.tunnel, event.pingCooldown)
				is AppEvent.SetTunnelPingInterval -> onPingIntervalChange(event.tunnel, event.pingInterval)
				is AppEvent.AddTunnelRunSSID -> onAddTunnelRunSSID(event.ssid, event.tunnel, state.tunnels)
				is AppEvent.DeleteTunnelRunSSID -> onRemoveTunnelRunSSID(event.ssid, event.tunnel)
				is AppEvent.ToggleEthernetTunnel -> onToggleEthernetTunnel(event.tunnel)
				is AppEvent.ToggleMobileDataTunnel -> onToggleMobileDataTunnel(event.tunnel)
				AppEvent.ToggleAutoTunnelOnCellular -> onToggleAutoTunnelOnCellular(state.appSettings)
				AppEvent.ToggleAutoTunnelOnWifi -> onToggleAutoTunnelOnWifi(state.appSettings)
				is AppEvent.DeleteTrustedSSID -> onDeleteTrustedSSID(event.ssid, state.appSettings)
				AppEvent.ToggleAutoTunnelWildcards -> onToggleAutoTunnelWildcards(state.appSettings)
				AppEvent.ToggleRootShellWifi -> onToggleRootShellWifi(state.appSettings)
				is AppEvent.SaveTrustedSSID -> onSaveTrustedSSID(event.ssid, state.appSettings)
				AppEvent.ToggleAutoTunnelOnEthernet -> onToggleTunnelOnEthernet(state.appSettings)
				AppEvent.ToggleStopKillSwitchOnTrusted -> onToggleStopKillSwitchOnTrusted(state.appSettings)
				AppEvent.ToggleStopTunnelOnNoInternet -> onToggleStopOnNoInternet(state.appSettings)
				is AppEvent.ExportTunnels -> onExportTunnels(event.configType, state.tunnels)
				AppEvent.ExportLogs -> onExportLogs()
				AppEvent.ErrorShown -> onErrorShown()
				AppEvent.BackStackPopped -> _appState.update { it.copy(popBackStack = false) }
				is AppEvent.TogglePingTunnelEnabled -> onTogglePingTunnel(event.tunnel)
				is AppEvent.SetTunnelPingIp -> onTunnelPingIpChange(event.tunnelConf, event.ip)
			}
		}
	}

	private fun collectLogs() {
		viewModelScope.launch(ioDispatcher) {
			logReader.bufferedLogs
				.runningFold(emptyList<LogMessage>()) { accumulator, log ->
					val updated = accumulator + log
					if (updated.size > Constants.LOG_BUFFER_SIZE) updated.takeLast(Constants.LOG_BUFFER_SIZE.toInt()) else updated
				}
				.collect { _logs.value = it }
		}
	}

	private suspend fun onTunnelPingIpChange(tunnelConf: TunnelConf, ip: String) = saveTunnel(
		tunnelConf.copy(pingIp = ip),
	)

	private suspend fun onTogglePingTunnel(tunnel: TunnelConf) = saveTunnel(
		tunnel.copy(isPingEnabled = !tunnel.isPingEnabled),
	)

	private suspend fun onToggleLocalLogging(currentlyEnabled: Boolean) {
		loggerMutex.withLock {
			val newEnabled = !currentlyEnabled
			appDataRepository.appState.setLocalLogsEnabled(newEnabled)
			withContext(mainDispatcher) {
				if (newEnabled) logReader.start() else logReader.stop()
			}
			if (!newEnabled) _logs.value = emptyList()
		}
	}

	private suspend fun onSetDebounceDelay(appSettings: AppSettings, delay: Int) = saveSettings(
		appSettings.copy(debounceDelaySeconds = delay),
	)

	private suspend fun onCopyTunnel(tunnel: TunnelConf, existingTunnels: List<TunnelConf>) = saveTunnel(
		TunnelConf(
			tunName = tunnel.generateUniqueName(existingTunnels.map { it.tunName }),
			wgQuick = tunnel.wgQuick,
			amQuick = tunnel.amQuick,
		),
	)

	private suspend fun onDeleteTunnel(tunnel: TunnelConf, state: AppUiState) {
		if (state.tunnels.size == 1 || tunnel.isPrimaryTunnel) {
			serviceManager.stopAutoTunnel()
		}
		appDataRepository.tunnels.delete(tunnel)
	}

	private suspend fun onStartTunnel(tunnel: TunnelConf) {
		tunControlMutex.withLock {
			tunnelManager.startTunnel(tunnel)
		}
	}

	private suspend fun onStopTunnel(tunnel: TunnelConf) {
		tunControlMutex.withLock {
			tunnelManager.stopTunnel(tunnel)
		}
	}

	private suspend fun onToggleAutoTunnel() {
		tunControlMutex.withLock {
			serviceManager.toggleAutoTunnel()
		}
	}

	private suspend fun onToggleExpandTunnelStats(currentlyEnabled: Boolean) {
		appDataRepository.appState.setTunnelStatsExpanded(!currentlyEnabled)
	}

	private fun onErrorShown() {
		_appState.update { it.copy(errorMessage = null) }
	}

	private fun onError(message: StringValue) {
		_appState.update { it.copy(errorMessage = message) }
	}

	private fun popBackStack() {
		_appState.update { it.copy(popBackStack = true) }
	}

	private suspend fun onImportTunnelFromFile(uri: Uri, tunnels: List<TunnelConf>) {
		runCatching {
			val tunnelConfigs = fileUtils.buildTunnelsFromUri(uri)
			val existingNames = tunnels.map { it.tunName }.toMutableList()
			val uniqueTunnelConfigs = tunnelConfigs.map { config ->
				val uniqueName = config.generateUniqueName(existingNames)
				existingNames.add(uniqueName)
				config.copy(tunName = uniqueName)
			}
			appDataRepository.tunnels.saveAll(uniqueTunnelConfigs)
		}.onFailure {
			// TODO handle exceptions, show message to UI
			Timber.e(it)
		}
	}

	private suspend fun onClipboardImport(config: String, tunnels: List<TunnelConf>) {
		runCatching {
			val amConfig = TunnelConf.configFromAmQuick(config)
			val tunnelConf = TunnelConf.tunnelConfigFromAmConfig(amConfig)
			saveTunnel(tunnelConf.copy(tunName = tunnelConf.generateUniqueName(tunnels.map { it.tunName })))
		}.onFailure {
			Timber.e(it)
			onError(StringValue.StringResource(R.string.error_file_format))
		}
	}

	private suspend fun onImportTunnelFromUrl(urlString: String, tunnels: List<TunnelConf>) {
		runCatching {
			val url = URL(urlString)
			val fileName = urlString.substringAfterLast("/")
			if (!fileName.endsWith(Constants.CONF_FILE_EXTENSION)) {
				throw InvalidFileExtensionException
			}
			url.openStream().use { stream ->
				val amConfig = Config.parse(stream)
				val tunnelConf = TunnelConf.tunnelConfigFromAmConfig(amConfig)
				saveTunnel(tunnelConf.copy(tunName = tunnelConf.generateUniqueName(tunnels.map { it.tunName })))
			}
		}.onFailure {
			Timber.e(it)
			val message = when (it) {
				is InvalidFileExtensionException -> StringValue.StringResource(R.string.error_file_extension)
				else -> StringValue.StringResource(R.string.error_download_failed)
			}
			onError(message)
		}
	}

	private suspend fun onImportTunnelFromQr(result: String, existingTunnels: List<TunnelConf>) {
		onClipboardImport(result, existingTunnels)
		popBackStack()
	}

	private suspend fun setBatteryOptimizeDisableShown() {
		appDataRepository.appState.setBatteryOptimizationDisableShown(true)
	}

	private fun initServicesFromSavedState(state: AppUiState) = viewModelScope.launch(ioDispatcher) {
		tunControlMutex.withLock {
			if (state.appSettings.isAutoTunnelEnabled) serviceManager.startAutoTunnel()
			state.tunnels.filter { it.isActive }.forEach {
				tunnelManager.startTunnel(it)
			}
		}
	}

	private fun initPin(enabled: Boolean) {
		if (enabled) PinManager.initialize(WireGuardAutoTunnel.instance)
	}

	private suspend fun onPinLockToggled(currentlyEnabled: Boolean) {
		if (currentlyEnabled) PinManager.clearPin()
		appDataRepository.appState.setPinLockEnabled(!currentlyEnabled)
	}

	private suspend fun setLocationDisclosureShown() {
		appDataRepository.appState.setLocationDisclosureShown(true)
	}

	private suspend fun onToggleAlwaysOnVPN(appSettings: AppSettings) = saveSettings(
		appSettings.copy(
			isAlwaysOnVpnEnabled = !appSettings.isAlwaysOnVpnEnabled,
		),
	)

	private suspend fun onLocaleChange(localeTag: String) {
		appDataRepository.appState.setLocale(localeTag)
		LocaleUtil.changeLocale(localeTag)
		_appState.update { it.copy(isConfigChanged = true) }
	}

	private suspend fun onToggleRestartAtBoot(appSettings: AppSettings) = saveSettings(
		appSettings.copy(
			isRestoreOnBootEnabled = !appSettings.isRestoreOnBootEnabled,
		),
	)

	private suspend fun onToggleVpnKillSwitch(appSettings: AppSettings) {
		val enabled = !appSettings.isVpnKillSwitchEnabled
		val updatedSettings = appSettings.copy(
			isVpnKillSwitchEnabled = enabled,
			isLanOnKillSwitchEnabled = if (enabled) appSettings.isLanOnKillSwitchEnabled else false,
		)
		saveSettings(updatedSettings)
		handleKillSwitchChange(updatedSettings)
	}

	private suspend fun onToggleLanOnKillSwitch(appSettings: AppSettings) {
		val updatedSettings = appSettings.copy(
			isLanOnKillSwitchEnabled = !appSettings.isLanOnKillSwitchEnabled,
		)
		saveSettings(updatedSettings)
		handleKillSwitchChange(appSettings)
	}

	private suspend fun handleKillSwitchChange(appSettings: AppSettings) {
		if (!appSettings.isVpnKillSwitchEnabled) return tunnelManager.setBackendState(BackendState.SERVICE_ACTIVE, emptyList())
		Timber.d("Starting kill switch")
		val allowedIps = if (appSettings.isLanOnKillSwitchEnabled) TunnelConf.LAN_BYPASS_ALLOWED_IPS else emptyList()
		tunnelManager.setBackendState(BackendState.KILL_SWITCH_ACTIVE, allowedIps)
	}

	private suspend fun onToggleAppShortcuts(appSettings: AppSettings) {
		val enabled = !appSettings.isShortcutsEnabled
		if (enabled) shortcutManager.addShortcuts() else shortcutManager.removeShortcuts()
		saveSettings(
			appSettings.copy(
				isShortcutsEnabled = enabled,
			),
		)
	}

	private suspend fun onTogglePrimaryTunnel(tunnelConf: TunnelConf) {
		tunnelMutex.withLock {
			appDataRepository.tunnels.updatePrimaryTunnel(
				when (tunnelConf.isPrimaryTunnel) {
					true -> null
					false -> tunnelConf
				},
			)
		}
	}

	private suspend fun onToggleIpv4(tunnelConf: TunnelConf) = saveTunnel(
		tunnelConf.copy(
			isIpv4Preferred = !tunnelConf.isIpv4Preferred,
		),
	)

	private suspend fun onPingIntervalChange(tunnelConf: TunnelConf, interval: String) = saveTunnel(
		tunnelConf.copy(pingInterval = if (interval.isBlank()) null else interval.toLong() * 1000),
	)

	private suspend fun onPingCoolDownChange(tunnelConf: TunnelConf, cooldown: String) = saveTunnel(
		tunnelConf.copy(pingCooldown = if (cooldown.isBlank()) null else cooldown.toLong() * 1000),
	)

	private suspend fun onThemeChange(theme: Theme) {
		appDataRepository.appState.setTheme(theme)
	}

	private suspend fun onToggleKernelMode(appSettings: AppSettings) {
		val enabled = !appSettings.isKernelEnabled
		if (enabled && !isKernelSupported()) {
			onError(StringValue.StringResource(R.string.kernel_not_supported))
			return
		}
		if (enabled && !requestRoot()) return
		tunnelManager.setBackendState(BackendState.INACTIVE, emptyList())
		saveSettings(appSettings.copy(isKernelEnabled = enabled))
	}

	private suspend fun onRemoveTunnelRunSSID(ssid: String, tunnelConfig: TunnelConf) = saveTunnel(
		tunnelConfig.copy(
			tunnelNetworks = (tunnelConfig.tunnelNetworks - ssid).toMutableList(),
		),
	)

	private suspend fun onAddTunnelRunSSID(ssid: String, tunnelConf: TunnelConf, existingTunnels: List<TunnelConf>) {
		if (ssid.isBlank()) return
		val trimmed = ssid.trim()
		if (existingTunnels.any { it.tunnelNetworks.contains(trimmed) }) return onError(StringValue.StringResource(R.string.error_ssid_exists))
		saveTunnel(
			tunnelConf.copy(
				tunnelNetworks = (tunnelConf.tunnelNetworks + ssid).toMutableList(),
			),
		)
	}

	private suspend fun onToggleMobileDataTunnel(tunnelConf: TunnelConf) {
		tunnelMutex.withLock {
			if (tunnelConf.isMobileDataTunnel) return appDataRepository.tunnels.updateMobileDataTunnel(null)
			appDataRepository.tunnels.updateMobileDataTunnel(tunnelConf)
		}
	}

	private suspend fun onToggleEthernetTunnel(tunnelConf: TunnelConf) {
		tunnelMutex.withLock {
			if (tunnelConf.isEthernetTunnel) return appDataRepository.tunnels.updateEthernetTunnel(null)
			appDataRepository.tunnels.updateEthernetTunnel(tunnelConf)
		}
	}

	private suspend fun onToggleAutoTunnelOnWifi(appSettings: AppSettings) = saveSettings(
		appSettings.copy(
			isTunnelOnWifiEnabled = !appSettings.isTunnelOnWifiEnabled,
		),
	)

	private suspend fun onToggleAutoTunnelOnCellular(appSettings: AppSettings) = saveSettings(
		appSettings.copy(
			isTunnelOnMobileDataEnabled = !appSettings.isTunnelOnMobileDataEnabled,
		),
	)

	private suspend fun onToggleAutoTunnelWildcards(appSettings: AppSettings) = saveSettings(
		appSettings.copy(
			isWildcardsEnabled = !appSettings.isWildcardsEnabled,
		),
	)

	private suspend fun onDeleteTrustedSSID(ssid: String, appSettings: AppSettings) = saveSettings(
		appSettings.copy(
			trustedNetworkSSIDs = (appSettings.trustedNetworkSSIDs - ssid).toMutableList(),
		),
	)

	private suspend fun onToggleRootShellWifi(appSettings: AppSettings) {
		if (requestRoot()) {
			saveSettings(
				appSettings.copy(isWifiNameByShellEnabled = !appSettings.isWifiNameByShellEnabled),
			)
		}
	}

	private suspend fun onToggleTunnelOnEthernet(appSettings: AppSettings) = saveSettings(
		appSettings.copy(isTunnelOnEthernetEnabled = !appSettings.isTunnelOnEthernetEnabled),
	)

	private suspend fun onSaveTrustedSSID(ssid: String, appSettings: AppSettings) {
		if (ssid.isEmpty()) return
		val trimmed = ssid.trim()
		if (appSettings.trustedNetworkSSIDs.contains(trimmed)) return onError(StringValue.StringResource(R.string.error_ssid_exists))
		saveSettings(
			appSettings.copy(
				trustedNetworkSSIDs = (appSettings.trustedNetworkSSIDs + ssid).toMutableList(),
			),
		)
	}

	private suspend fun onToggleStopOnNoInternet(appSettings: AppSettings) = saveSettings(
		appSettings.copy(isStopOnNoInternetEnabled = !appSettings.isStopOnNoInternetEnabled),
	)

	private suspend fun onToggleStopKillSwitchOnTrusted(appSettings: AppSettings) = saveSettings(
		appSettings.copy(isDisableKillSwitchOnTrustedEnabled = !appSettings.isDisableKillSwitchOnTrustedEnabled),
	)

	private suspend fun isKernelSupported(): Boolean {
		return withContext(ioDispatcher) {
			WgQuickBackend.hasKernelSupport()
		}
	}

	private suspend fun saveSettings(appSettings: AppSettings) = withContext(ioDispatcher) {
		settingsMutex.withLock {
			appDataRepository.settings.save(appSettings)
		}
	}

	private suspend fun saveTunnel(tunnel: TunnelConf) = withContext(ioDispatcher) {
		tunnelMutex.withLock {
			appDataRepository.tunnels.save(tunnel)
		}
	}

	private suspend fun onExportTunnels(configType: ConfigType, tunnels: List<TunnelConf>) {
		runCatching {
			val (files, shareFileName) = when (configType) {
				ConfigType.AMNEZIA -> {
					Pair(fileUtils.createAmFiles(tunnels), "am-export_${Instant.now().epochSecond}.zip")
				}
				ConfigType.WG -> {
					Pair(fileUtils.createWgFiles(tunnels), "wg-export_${Instant.now().epochSecond}.zip")
				}
			}
			val shareFile = fileUtils.createNewShareFile(shareFileName)
			fileUtils.zipAll(shareFile, files)
			fileUtils.shareFile(shareFile)
		}.onFailure {
			// TODO handle error
			Timber.e(it)
		}
	}

	private suspend fun onExportLogs() {
		runCatching {
			val file = fileUtils.createNewShareFile("${Constants.BASE_LOG_FILE_NAME}-${Instant.now().epochSecond}.zip")
			logReader.zipLogFiles(file.absolutePath)
			fileUtils.shareFile(file)
		}.onFailure {
			// TODO handle error
			Timber.e(it)
		}
	}

	private suspend fun requestRoot(): Boolean {
		return withContext(ioDispatcher) {
			try {
				rootShell.get().start()
				SnackbarController.showMessage(StringValue.StringResource(R.string.root_accepted))
				true
			} catch (e: Exception) {
				SnackbarController.showMessage(StringValue.StringResource(R.string.error_root_denied))
				false
			}
		}
	}
}
