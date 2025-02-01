package com.zaneschepke.wireguardautotunnel.service.tunnel

import com.zaneschepke.wireguardautotunnel.data.domain.TunnelConfig
import com.zaneschepke.wireguardautotunnel.data.repository.AppDataRepository
import com.zaneschepke.wireguardautotunnel.module.ApplicationScope
import com.zaneschepke.wireguardautotunnel.module.IoDispatcher
import com.zaneschepke.wireguardautotunnel.service.foreground.ServiceManager
import com.zaneschepke.wireguardautotunnel.service.network.NetworkService
import com.zaneschepke.wireguardautotunnel.service.tunnel.model.BackendState
import com.zaneschepke.wireguardautotunnel.service.tunnel.model.TunnelState
import com.zaneschepke.wireguardautotunnel.service.tunnel.model.VpnState
import com.zaneschepke.wireguardautotunnel.service.tunnel.statistics.AmneziaStatistics
import com.zaneschepke.wireguardautotunnel.util.extensions.asAmBackendState
import com.zaneschepke.wireguardautotunnel.util.extensions.asBackendState
import com.zaneschepke.wireguardautotunnel.util.extensions.asTunnelState
import com.zaneschepke.wireguardautotunnel.util.extensions.cancelWithMessage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.amnezia.awg.backend.Backend
import org.amnezia.awg.backend.Tunnel
import timber.log.Timber
import javax.inject.Inject

class UserspaceTunnel @Inject constructor(@IoDispatcher private val ioDispatcher: CoroutineDispatcher,
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

		override suspend fun startTunnel() {
			withContext(ioDispatcher) {
				if (getState().isUp()) return@withContext
				serviceManager.startBackgroundService(tunnelConfig)
				appDataRepository.tunnels.save(tunnelConfig.copy(isActive = true))
				runCatching {
					val state = backend.setState(this@UserspaceTunnel, Tunnel.State.UP, tunnelConfig.toAmConfig())
					updateTunnelState(state.asTunnelState())
					startActiveTunnelJobs()
				}.onFailure {
					onTunnelStop()
					Timber.e(it)
				}
			}
		}

		private fun startStateJob() : Job = applicationScope.launch(ioDispatcher) {
			state.collect {
				onVpnStateChange(tunnelConfig,it)
			}
		}

		override fun startActiveTunnelJobs() {
			stateJob = startStateJob()
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
				Timber.d("Stopping tun!")
				backend.setState(this@UserspaceTunnel, Tunnel.State.DOWN, tunnelConfig.toAmConfig())
				onTunnelStop()
			}.onFailure {
				Timber.e(it)
			}
		}

		private fun startTunnelStatisticsJob() = applicationScope.launch(ioDispatcher) {
			delay(STATS_START_DELAY)
			while (true) {
				val stats = backend.getStatistics(this@UserspaceTunnel)
				updateBackendStatistics(AmneziaStatistics(stats))
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
			resetState()
			onStop(tunnelConfig)
		}

		private suspend fun toggleTunnel(state: Tunnel.State) {
			withContext(ioDispatcher) {
				backend.setState(this@UserspaceTunnel, state, tunnelConfig.toAmConfig())
			}
		}

		override suspend fun getBackendState(): BackendState {
			return backend.backendState.asBackendState()
		}

		override suspend fun setBackendState(
			backendState: BackendState,
			allowedIps: Collection<String>,
		) {
			backend.setBackendState(backendState.asAmBackendState(), allowedIps)
			state.update {
				it.copy(backendState = backendState)
			}
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

	override fun isIpv4ResolutionPreferred(): Boolean {
		return tunnelConfig.isIpv4Preferred
	}

	override fun onStateChange(newState: Tunnel.State) {
			state.update {
				it.copy(state = newState.asTunnelState())
			}
			serviceManager.updateTunnelTile()
		}
}
