package com.zaneschepke.wireguardautotunnel.service.tunnel

import com.wireguard.android.backend.Backend
import com.wireguard.android.backend.Tunnel
import com.zaneschepke.wireguardautotunnel.data.domain.TunnelConfig
import com.zaneschepke.wireguardautotunnel.data.repository.AppDataRepository
import com.zaneschepke.wireguardautotunnel.module.ApplicationScope
import com.zaneschepke.wireguardautotunnel.module.IoDispatcher
import com.zaneschepke.wireguardautotunnel.service.foreground.ServiceManager
import com.zaneschepke.wireguardautotunnel.service.network.NetworkService
import com.zaneschepke.wireguardautotunnel.service.tunnel.model.BackendState
import com.zaneschepke.wireguardautotunnel.service.tunnel.model.TunnelState
import com.zaneschepke.wireguardautotunnel.service.tunnel.model.VpnState
import com.zaneschepke.wireguardautotunnel.service.tunnel.statistics.WireGuardStatistics
import com.zaneschepke.wireguardautotunnel.util.extensions.asTunnelState
import com.zaneschepke.wireguardautotunnel.util.extensions.cancelWithMessage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

class KernelTunnel @Inject constructor(
	@IoDispatcher private val ioDispatcher: CoroutineDispatcher,
	@ApplicationScope private val applicationScope: CoroutineScope,
	private val serviceManager: ServiceManager,
	private val appDataRepository: AppDataRepository,
	private val backend: Backend,
	private val internetConnectivityService: NetworkService,
	override var tunnelConfig: TunnelConfig,
	private val onVpnStateChange: (tunnelConfig: TunnelConfig,state: VpnState) -> Unit,
	private val onStop: (tunnelConfig : TunnelConfig) -> Unit,
) : TunnelService(), Tunnel {

	private var statsJob: Job? = null
	private var tunnelChangesJob: Job? = null
	private var pingJob: Job? = null
	private var networkJob: Job? = null
	private var stateJob: Job? = null

	private fun startStateJob() : Job = applicationScope.launch(ioDispatcher) {
		state.collect {
			onVpnStateChange(tunnelConfig,it)
		}
	}

	override suspend fun startTunnel() {
		withContext(ioDispatcher) {
			if (runningTunnelNames().contains(tunnelConfig.name)) return@withContext
			serviceManager.startBackgroundService(tunnelConfig)
			stateJob = startStateJob()
			appDataRepository.tunnels.save(tunnelConfig.copy(isActive = true))
			runCatching {
				val state = backend.setState(this@KernelTunnel, Tunnel.State.UP, tunnelConfig.toWgConfig())
				updateTunnelState(state.asTunnelState())
				startActiveTunnelJobs()
			}.onFailure {
				Timber.e(it)
			}
		}
	}

	override fun startActiveTunnelJobs() {
		statsJob = startTunnelStatisticsJob()
		tunnelChangesJob = startTunnelConfigChangesJob()
		if (tunnelConfig.isPingEnabled == true) {
			startPingJobs()
		}
	}

	override fun cancelActiveTunnelJobs() {
		stateJob?.cancelWithMessage("Tunnel state job canceled")
		statsJob?.cancelWithMessage("Tunnel stats job canceled")
		tunnelChangesJob?.cancelWithMessage("Tunnel changes job canceled")
		cancelPingJobs()
	}

	private fun startPingJobs() {
		cancelPingJobs()
		pingJob = startPingJob()
		networkJob = startNetworkJob()
	}

	private fun cancelPingJobs() {
		pingJob?.cancelWithMessage("Ping job canceled")
		networkJob?.cancelWithMessage("Network job canceled")
	}

	private fun startPingJob() = applicationScope.launch(ioDispatcher) {
		do {
			runTunnelPingCheck()
		} while (true)
	}

	private fun startNetworkJob() = applicationScope.launch(ioDispatcher) {
		internetConnectivityService.status.distinctUntilChanged().collect {
			isNetworkAvailable.set(!it.allOffline)
		}
	}

	override suspend fun stopTunnel() {
		runCatching {
			backend.setState(this@KernelTunnel, Tunnel.State.DOWN, tunnelConfig.toWgConfig())
			onTunnelStop()
		}.onFailure {
			Timber.e(it)
		}
}

	private fun startTunnelStatisticsJob() = applicationScope.launch(ioDispatcher) {
		delay(STATS_START_DELAY)
		while (true) {
			val stats = backend.getStatistics(this@KernelTunnel)
			updateBackendStatistics(WireGuardStatistics(stats))
			delay(VPN_STATISTIC_CHECK_INTERVAL)
		}
	}

	private fun startTunnelConfigChangesJob() = applicationScope.launch(ioDispatcher) {
		appDataRepository.tunnels.getTunnelConfigsFlow().collect { tunnels ->
			val storageConfig = tunnels.firstOrNull { it.id == tunnelConfig.id }
			if (storageConfig == null) return@collect
			val quickChanged = isQuickConfigChanged(storageConfig)
			val pingMatching = isPingConfigMatching(storageConfig)
			safeUpdateConfig(storageConfig)
			if (quickChanged) bounceTunnel()
			if (!pingMatching) handlePingConfigChanges()
		}
	}

	private fun restartPingJob() {
		cancelPingJobs()
		startPingJobs()
	}

	private fun handlePingConfigChanges() {
		if (!tunnelConfig.isPingEnabled && pingJob?.isActive == true) {
			cancelPingJobs()
			return
		}
		restartPingJob()
	}

	override suspend fun bounceTunnel() {
		if (getState().isUp()) {
			toggleTunnel(Tunnel.State.DOWN)
			toggleTunnel(Tunnel.State.UP)
		}
	}

	private suspend fun onTunnelStop() {
		appDataRepository.tunnels.save(tunnelConfig.copy(isActive = false))
		serviceManager.stopBackgroundService()
		cancelActiveTunnelJobs()
		onStop(tunnelConfig)
	}

	private suspend fun toggleTunnel(state: Tunnel.State) {
		withContext(ioDispatcher) {
			backend.setState(this@KernelTunnel, state, tunnelConfig.toWgConfig())
		}
	}

	override suspend fun getBackendState(): BackendState {
		// TODO Not implemented for kernel
		return BackendState.INACTIVE
	}

	override suspend fun setBackendState(backendState: BackendState, allowedIps: Collection<String>) {
	}
	override suspend fun runningTunnelNames(): Set<String> {
		return backend.runningTunnelNames
	}

	override suspend fun getState(): TunnelState {
		return backend.getState(this).asTunnelState()
	}

	override fun getName(): String {
		return tunnelConfig.name
	}

	override fun onStateChange(newState: Tunnel.State) {
		updateTunnelState(newState.asTunnelState())
		serviceManager.updateTunnelTile()
	}

	override fun isIpv4ResolutionPreferred(): Boolean {
		return false
	}
}
