package com.zaneschepke.wireguardautotunnel.service.tunnel

import com.wireguard.android.backend.Backend
import com.wireguard.android.backend.Tunnel.State
import com.zaneschepke.wireguardautotunnel.WireGuardAutoTunnel
import com.zaneschepke.wireguardautotunnel.data.domain.TunnelConfig
import com.zaneschepke.wireguardautotunnel.data.repository.AppDataRepository
import com.zaneschepke.wireguardautotunnel.data.repository.TunnelConfigRepository
import com.zaneschepke.wireguardautotunnel.module.ApplicationScope
import com.zaneschepke.wireguardautotunnel.module.Ethernet
import com.zaneschepke.wireguardautotunnel.module.IoDispatcher
import com.zaneschepke.wireguardautotunnel.module.Kernel
import com.zaneschepke.wireguardautotunnel.module.MobileData
import com.zaneschepke.wireguardautotunnel.module.Wifi
import com.zaneschepke.wireguardautotunnel.service.foreground.ServiceManager
import com.zaneschepke.wireguardautotunnel.service.foreground.autotunnel.model.NetworkState
import com.zaneschepke.wireguardautotunnel.service.network.NetworkService
import com.zaneschepke.wireguardautotunnel.service.notification.NotificationService
import com.zaneschepke.wireguardautotunnel.service.notification.NotificationService.Companion.VPN_NOTIFICATION_ID
import com.zaneschepke.wireguardautotunnel.service.tunnel.statistics.AmneziaStatistics
import com.zaneschepke.wireguardautotunnel.service.tunnel.statistics.TunnelStatistics
import com.zaneschepke.wireguardautotunnel.service.tunnel.statistics.WireGuardStatistics
import com.zaneschepke.wireguardautotunnel.ui.common.snackbar.SnackbarController
import com.zaneschepke.wireguardautotunnel.util.Constants
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.service.notification.WireGuardNotification
import com.zaneschepke.wireguardautotunnel.util.StringValue
import com.zaneschepke.wireguardautotunnel.util.extensions.asAmBackendState
import com.zaneschepke.wireguardautotunnel.util.extensions.asBackendState
import com.zaneschepke.wireguardautotunnel.util.extensions.cancelWithMessage
import com.zaneschepke.wireguardautotunnel.util.extensions.isReachable
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.amnezia.awg.backend.Tunnel
import timber.log.Timber
import java.net.InetAddress
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Provider

class WireGuardTunnel
@Inject
constructor(
	private val amneziaBackend: Provider<org.amnezia.awg.backend.Backend>,
	private val tunnelConfigRepository: TunnelConfigRepository,
	@Kernel private val kernelBackend: Provider<Backend>,
	private val appDataRepository: AppDataRepository,
	@ApplicationScope private val applicationScope: CoroutineScope,
	@IoDispatcher private val ioDispatcher: CoroutineDispatcher,
	private val serviceManager: ServiceManager,
	private val notificationService: NotificationService,
	@Wifi private val wifiService: NetworkService,
	@MobileData private val mobileDataService: NetworkService,
	@Ethernet private val ethernetService: NetworkService,
) : TunnelService {

	private val _vpnState = MutableStateFlow(VpnState())
	override val vpnState: StateFlow<VpnState> = _vpnState.asStateFlow()

	private var statsJob: Job? = null
	private var tunnelChangesJob: Job? = null
	private var pingJob: Job? = null
	private var networkJob: Job? = null

	@get:Synchronized @set:Synchronized
	private var isKernelBackend: Boolean? = null
	private val isNetworkAvailable = AtomicBoolean(false)

	private val tunnelControlMutex = Mutex()

	init {
		applicationScope.launch(ioDispatcher) {
			appDataRepository.settings.getSettingsFlow().collect {
				isKernelBackend = it.isKernelEnabled
			}
		}
	}

	private suspend fun backend(): Any {
		val isKernelEnabled = isKernelBackend
			?: appDataRepository.settings.getSettings().isKernelEnabled
		if (isKernelEnabled) return kernelBackend.get()
		return amneziaBackend.get()
	}

	override suspend fun runningTunnelNames(): Set<String> {
		return when (val backend = backend()) {
			is Backend -> backend.runningTunnelNames
			is org.amnezia.awg.backend.Backend -> backend.runningTunnelNames
			else -> emptySet()
		}
	}

	// TODO refactor duplicate
	@OptIn(FlowPreview::class)
	private fun combineNetworkEventsJob(): Flow<NetworkState> {
		return combine(
			wifiService.status,
			mobileDataService.status,
			ethernetService.status,
		) { wifi, mobileData, ethernet ->
			NetworkState(
				wifi.available,
				mobileData.available,
				ethernet.available,
				wifi.name,
			)
		}.distinctUntilChanged()
	}

	private suspend fun setState(tunnelConfig: TunnelConfig, tunnelState: TunnelState): Result<TunnelState> {
		return runCatching {
			when (val backend = backend()) {
				is Backend -> backend.setState(this, tunnelState.toWgState(), tunnelConfig.toWgConfig()).let { TunnelState.from(it) }
				is org.amnezia.awg.backend.Backend -> {
					backend.setState(this, tunnelState.toAmState(), tunnelConfig.toAmConfig()).let {
						TunnelState.from(it)
					}
				}
				else -> throw NotImplementedError()
			}
		}.onFailure {
			// TODO add better error message to user, especially for kernel as exceptions contain no details
			Timber.e(it)
		}
	}

	private fun isTunnelAlreadyRunning(tunnelConfig: TunnelConfig): Boolean {
		return with(_vpnState.value) {
			this.tunnelConfig?.id == tunnelConfig.id && status.isUp().also {
				if (it) Timber.w("Tunnel already running")
			}
		}
	}

	override suspend fun startTunnel(tunnelConfig: TunnelConfig?) {
		withContext(ioDispatcher) {
			if (tunnelConfig == null || isTunnelAlreadyRunning(tunnelConfig)) return@withContext
			onBeforeStart(tunnelConfig)
			updateTunnelConfig(tunnelConfig) // need to update this here
			appDataRepository.tunnels.save(tunnelConfig.copy(isActive = true))
			withServiceActive {
				setState(tunnelConfig, TunnelState.UP).onSuccess {
					updateTunnelState(it, tunnelConfig)
					startActiveTunnelJobs()
				}.onFailure {
					onTunnelStop(tunnelConfig)
					// TODO improve this with better statuses and handling
					showTunnelStartFailed()
				}
			}
		}
	}

	private fun showTunnelStartFailed() {
		if (WireGuardAutoTunnel.isForeground()) {
			SnackbarController.showMessage(StringValue.StringResource(R.string.error_tunnel_start))
		} else {
			launchStartFailedNotification()
		}
	}

	private fun launchStartFailedNotification() {
		with(notificationService) {
			val notification = createNotification(
				WireGuardNotification.NotificationChannels.VPN,
				title = context.getString(R.string.error_tunnel_start),
			)
			show(VPN_NOTIFICATION_ID, notification)
		}
	}

	override suspend fun stopTunnel() {
		withContext(ioDispatcher) {
			if (_vpnState.value.status.isDown()) return@withContext
			with(_vpnState.value) {
				if (tunnelConfig == null) return@withContext
				setState(tunnelConfig, TunnelState.DOWN).onSuccess {
					onTunnelStop(tunnelConfig)
					updateTunnelState(it, null)
				}.onFailure {
					clearJobsAndStats()
					Timber.e(it)
				}
			}
		}
	}

	private suspend fun toggleTunnel(tunnelConfig: TunnelConfig) {
		withContext(ioDispatcher) {
			tunnelControlMutex.withLock {
				setState(tunnelConfig, TunnelState.TOGGLE)
			}
		}
	}

	// utility to keep vpnService alive during rapid changes to prevent bad states
	private suspend fun withServiceActive(callback: suspend () -> Unit) {
		when (val backend = backend()) {
			is org.amnezia.awg.backend.Backend -> {
				val backendState = backend.backendState
				if (backendState == org.amnezia.awg.backend.Backend.BackendState.INACTIVE) {
					backend.setBackendState(org.amnezia.awg.backend.Backend.BackendState.SERVICE_ACTIVE, emptyList())
				}
				callback()
			}
			is Backend -> callback()
		}
	}

	override suspend fun bounceTunnel() {
		with(_vpnState.value) {
			if (tunnelConfig != null && status.isUp()) {
				withServiceActive {
					toggleTunnel(tunnelConfig)
					toggleTunnel(tunnelConfig)
				}
			}
		}
	}

	override suspend fun getBackendState(): BackendState {
		return when (val backend = backend()) {
			is org.amnezia.awg.backend.Backend -> {
				backend.backendState.asBackendState()
			}
			is Backend -> BackendState.SERVICE_ACTIVE
			else -> BackendState.INACTIVE
		}
	}

	override suspend fun setBackendState(backendState: BackendState, allowedIps: Collection<String>) {
		kotlin.runCatching {
			when (val backend = backend()) {
				is org.amnezia.awg.backend.Backend -> {
					backend.setBackendState(backendState.asAmBackendState(), allowedIps)
				}
				is Backend -> {
					// TODO not yet implemented
					Timber.d("Kernel backend state not yet implemented")
				}
				else -> Unit
			}
		}
	}

	private suspend fun onBeforeStart(tunnelConfig: TunnelConfig) {
		with(_vpnState.value) {
			if (status.isUp()) stopTunnel() else clearJobsAndStats()
			serviceManager.startBackgroundService(tunnelConfig)
		}
	}

	private suspend fun onTunnelStop(tunnelConfig: TunnelConfig) {
		runCatching {
			appDataRepository.tunnels.save(tunnelConfig.copy(isActive = false))
			serviceManager.stopBackgroundService()
			notificationService.remove(VPN_NOTIFICATION_ID)
			clearJobsAndStats()
		}
	}

	private fun clearJobsAndStats() {
		cancelActiveTunnelJobs()
		resetBackendStatistics()
	}

	private fun updateTunnelState(state: TunnelState, tunnelConfig: TunnelConfig?) {
		_vpnState.update {
			it.copy(status = state, tunnelConfig = tunnelConfig)
		}
	}

	private fun updateTunnelConfig(tunnelConfig: TunnelConfig?) {
		_vpnState.update {
			it.copy(tunnelConfig = tunnelConfig)
		}
	}

	private fun updateBackendStatistics(statistics: TunnelStatistics) {
		_vpnState.update {
			it.copy(statistics = statistics)
		}
	}

	private fun resetBackendStatistics() {
		_vpnState.update {
			it.copy(statistics = null)
		}
	}

	override suspend fun getState(): TunnelState {
		return when (val backend = backend()) {
			is Backend -> backend.getState(this).let { TunnelState.from(it) }
			is org.amnezia.awg.backend.Backend -> backend.getState(this).let { TunnelState.from(it) }
			else -> TunnelState.DOWN
		}
	}

	override fun cancelActiveTunnelJobs() {
		statsJob?.cancelWithMessage("Tunnel stats job cancelled")
		tunnelChangesJob?.cancelWithMessage("Tunnel changes job cancelled")
		cancelPingJobs()
	}

	override fun startActiveTunnelJobs() {
		statsJob = startTunnelStatisticsJob()
		tunnelChangesJob = startTunnelConfigChangesJob()
		if (_vpnState.value.tunnelConfig?.isPingEnabled == true) {
			startPingJobs()
		}
	}

	private fun startPingJobs() {
		cancelPingJobs()
		pingJob = startPingJob()
		networkJob = startNetworkJob()
	}
	override fun getName(): String {
		return _vpnState.value.tunnelConfig?.name ?: ""
	}

	private fun startTunnelStatisticsJob() = applicationScope.launch(ioDispatcher) {
		val backend = backend()
		delay(STATS_START_DELAY)
		while (true) {
			when (backend) {
				is Backend -> updateBackendStatistics(
					WireGuardStatistics(backend.getStatistics(this@WireGuardTunnel)),
				)
				is org.amnezia.awg.backend.Backend -> updateBackendStatistics(
					AmneziaStatistics(
						backend.getStatistics(this@WireGuardTunnel),
					),
				)
			}
			delay(VPN_STATISTIC_CHECK_INTERVAL)
		}
	}

	private fun isQuickConfigChanged(config: TunnelConfig): Boolean {
		return with(_vpnState.value) {
			if (tunnelConfig == null) return false
			config.wgQuick != tunnelConfig.wgQuick ||
				config.amQuick != tunnelConfig.amQuick
		}
	}

	private fun isPingConfigMatching(config: TunnelConfig): Boolean {
		return with(_vpnState.value.tunnelConfig) {
			if (this == null) return true
			config.isPingEnabled == isPingEnabled &&
				pingIp == config.pingIp &&
				config.pingCooldown == pingCooldown &&
				config.pingInterval == pingInterval
		}
	}

	private fun handlePingConfigChanges() {
		with(_vpnState.value.tunnelConfig) {
			if (this == null) return
			if (!isPingEnabled && pingJob?.isActive == true) {
				cancelPingJobs()
				return
			}
			restartPingJob()
		}
	}

	private fun restartPingJob() {
		cancelPingJobs()
		startPingJobs()
	}

	private fun cancelPingJobs() {
		pingJob?.cancelWithMessage("Ping job cancelled")
		networkJob?.cancelWithMessage("Network job cancelled")
	}

	private fun startTunnelConfigChangesJob() = applicationScope.launch(ioDispatcher) {
		tunnelConfigRepository.getTunnelConfigsFlow().collect { tunnels ->
			with(_vpnState.value) {
				if (tunnelConfig == null) return@collect
				val storageConfig = tunnels.firstOrNull { it.id == tunnelConfig.id }
				if (storageConfig == null) return@collect
				val quickChanged = isQuickConfigChanged(storageConfig)
				val pingMatching = isPingConfigMatching(storageConfig)
				updateTunnelConfig(storageConfig)
				if (quickChanged) bounceTunnel()
				if (!pingMatching) handlePingConfigChanges()
			}
		}
	}

	private suspend fun pingTunnel(tunnelConfig: TunnelConfig): List<Boolean> {
		return withContext(ioDispatcher) {
			val config = tunnelConfig.toWgConfig()
			if (tunnelConfig.pingIp != null) {
				Timber.i("Pinging custom ip")
				listOf(InetAddress.getByName(tunnelConfig.pingIp).isReachable(Constants.PING_TIMEOUT.toInt()))
			} else {
				Timber.i("Pinging all peers")
				config.peers.map { peer ->
					peer.isReachable()
				}
			}
		}
	}

	private fun startPingJob() = applicationScope.launch(ioDispatcher) {
		do {
			run {
				with(_vpnState.value) {
					if (status.isUp() && tunnelConfig != null && isNetworkAvailable.get()) {
						val reachable = pingTunnel(tunnelConfig)
						if (reachable.contains(false)) {
							if (isNetworkAvailable.get()) {
								Timber.i("Ping result: target was not reachable, bouncing the tunnel")
								bounceTunnel()
								delay(tunnelConfig.pingCooldown ?: Constants.PING_COOLDOWN)
							} else {
								Timber.i("Ping result: target was not reachable, but not network available")
							}
							return@run
						} else {
							Timber.i("Ping result: all ping targets were reached successfully")
						}
					}
					delay(tunnelConfig?.pingInterval ?: Constants.PING_INTERVAL)
				}
			}
		} while (true)
	}

	private fun startNetworkJob() = applicationScope.launch(ioDispatcher) {
		combineNetworkEventsJob().collect {
			Timber.d("New network state: $it")
			if (!it.isWifiConnected && !it.isEthernetConnected && !it.isMobileDataConnected) {
				isNetworkAvailable.set(false)
			} else {
				isNetworkAvailable.set(true)
			}
		}
	}

	override fun onStateChange(newState: Tunnel.State) {
		_vpnState.update {
			it.copy(status = TunnelState.from(newState))
		}
		serviceManager.updateTunnelTile()
	}

	override fun onStateChange(state: State) {
		_vpnState.update {
			it.copy(status = TunnelState.from(state))
		}
		serviceManager.updateTunnelTile()
	}

	companion object {
		const val STATS_START_DELAY = 1_000L
		const val VPN_STATISTIC_CHECK_INTERVAL = 1_000L
	}
}
