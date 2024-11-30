package com.zaneschepke.wireguardautotunnel.service.tunnel

import com.wireguard.android.backend.Backend
import com.wireguard.android.backend.Tunnel.State
import com.zaneschepke.wireguardautotunnel.data.domain.TunnelConfig
import com.zaneschepke.wireguardautotunnel.data.repository.AppDataRepository
import com.zaneschepke.wireguardautotunnel.data.repository.TunnelConfigRepository
import com.zaneschepke.wireguardautotunnel.module.ApplicationScope
import com.zaneschepke.wireguardautotunnel.module.IoDispatcher
import com.zaneschepke.wireguardautotunnel.module.Kernel
import com.zaneschepke.wireguardautotunnel.service.foreground.ServiceManager
import com.zaneschepke.wireguardautotunnel.service.tunnel.statistics.AmneziaStatistics
import com.zaneschepke.wireguardautotunnel.service.tunnel.statistics.TunnelStatistics
import com.zaneschepke.wireguardautotunnel.service.tunnel.statistics.WireGuardStatistics
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.amnezia.awg.backend.Tunnel
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Provider

class WireGuardTunnel
@Inject
constructor(
	private val amneziaBackend: Provider<org.amnezia.awg.backend.Backend>,
	tunnelConfigRepository: TunnelConfigRepository,
	@Kernel private val kernelBackend: Provider<Backend>,
	private val appDataRepository: AppDataRepository,
	@ApplicationScope private val applicationScope: CoroutineScope,
	@IoDispatcher private val ioDispatcher: CoroutineDispatcher,
	private val serviceManager: ServiceManager,
) : TunnelService {

	private val _vpnState = MutableStateFlow(VpnState())
	override val vpnState: StateFlow<VpnState> = _vpnState.combine(
		tunnelConfigRepository.getTunnelConfigsFlow(),
	) {
			vpnState, tunnels ->
		vpnState.copy(
			tunnelConfig = tunnels.firstOrNull { it.id == vpnState.tunnelConfig?.id },
		)
	}.stateIn(applicationScope, SharingStarted.Eagerly, VpnState())

	private var statsJob: Job? = null

	private val mutex = Mutex()

	private suspend fun backend(): Any {
		val settings = appDataRepository.settings.getSettings()
		if (settings.isKernelEnabled) return kernelBackend.get()
		return amneziaBackend.get()
	}

	override suspend fun runningTunnelNames(): Set<String> {
		return when (val backend = backend()) {
			is Backend -> backend.runningTunnelNames
			is org.amnezia.awg.backend.Backend -> backend.runningTunnelNames
			else -> emptySet()
		}
	}

	private suspend fun setState(tunnelConfig: TunnelConfig, tunnelState: TunnelState): Result<TunnelState> {
		return runCatching {
			when (val backend = backend()) {
				is Backend -> backend.setState(this, tunnelState.toWgState(), TunnelConfig.configFromWgQuick(tunnelConfig.wgQuick)).let { TunnelState.from(it) }
				is org.amnezia.awg.backend.Backend -> {
					val config = if (tunnelConfig.amQuick.isBlank()) {
						TunnelConfig.configFromAmQuick(
							tunnelConfig.wgQuick,
						)
					} else {
						TunnelConfig.configFromAmQuick(tunnelConfig.amQuick)
					}
					backend.setState(this, tunnelState.toAmState(), config).let {
						TunnelState.from(it)
					}
				}
				else -> throw NotImplementedError()
			}
		}.onFailure {
			Timber.e(it)
		}
	}

	private fun isTunnelAlreadyRunning(tunnelConfig: TunnelConfig): Boolean {
		val isRunning = tunnelConfig == _vpnState.value.tunnelConfig && _vpnState.value.status.isUp()
		if (isRunning) Timber.w("Tunnel already running")
		return isRunning
	}

	override suspend fun startTunnel(tunnelConfig: TunnelConfig?, background: Boolean) {
		if (tunnelConfig == null) return
		withContext(ioDispatcher) {
			mutex.withLock {
				if (isTunnelAlreadyRunning(tunnelConfig)) return@withContext
				onBeforeStart(background)
				setState(tunnelConfig, TunnelState.UP).onSuccess {
					startStatsJob()
					if (it.isUp()) appDataRepository.tunnels.save(tunnelConfig.copy(isActive = true))
					updateTunnelState(it, tunnelConfig)
				}.onFailure {
					Timber.e(it)
				}
			}
		}
	}

	override suspend fun stopTunnel() {
		withContext(ioDispatcher) {
			mutex.withLock {
				if (_vpnState.value.status.isDown()) return@withContext
				with(_vpnState.value) {
					if (tunnelConfig == null) return@withContext
					setState(tunnelConfig, TunnelState.DOWN).onSuccess {
						updateTunnelState(it, null)
						onStop(tunnelConfig)
						stopBackgroundService()
					}.onFailure {
						Timber.e(it)
					}
				}
			}
		}
	}

	override suspend fun bounceTunnel() {
		if (_vpnState.value.tunnelConfig == null) return
		val config = _vpnState.value.tunnelConfig
		stopTunnel()
		startTunnel(config)
	}

	private suspend fun shutDownActiveTunnel() {
		with(_vpnState.value) {
			if (status.isUp()) {
				stopTunnel()
			}
		}
	}

	private suspend fun startBackgroundService() {
		serviceManager.startBackgroundService()
		serviceManager.requestTunnelTileUpdate()
	}

	private fun stopBackgroundService() {
		serviceManager.stopBackgroundService()
		serviceManager.requestTunnelTileUpdate()
	}

	private suspend fun onBeforeStart(background: Boolean) {
		shutDownActiveTunnel()
		resetBackendStatistics()
		val settings = appDataRepository.settings.getSettings()
		if (background || settings.isKernelEnabled) startBackgroundService()
	}

	private suspend fun onStop(tunnelConfig: TunnelConfig) {
		appDataRepository.tunnels.save(tunnelConfig.copy(isActive = false))
		cancelStatsJob()
		resetBackendStatistics()
	}

	private fun updateTunnelState(state: TunnelState, tunnelConfig: TunnelConfig?) {
		_vpnState.update {
			it.copy(status = state, tunnelConfig = tunnelConfig)
		}
	}

	private fun emitBackendStatistics(statistics: TunnelStatistics) {
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

	override fun cancelStatsJob() {
		statsJob?.cancel()
	}

	override fun startStatsJob() {
		statsJob = startTunnelStatisticsJob()
	}

	override fun getName(): String {
		return _vpnState.value.tunnelConfig?.name ?: ""
	}

	private fun startTunnelStatisticsJob() = applicationScope.launch(ioDispatcher) {
		val backend = backend()
		delay(STATS_START_DELAY)
		while (true) {
			when (backend) {
				is Backend -> emitBackendStatistics(
					WireGuardStatistics(backend.getStatistics(this@WireGuardTunnel)),
				)
				is org.amnezia.awg.backend.Backend -> {
					emitBackendStatistics(
						AmneziaStatistics(
							backend.getStatistics(this@WireGuardTunnel),
						),
					)
				}
			}
			delay(VPN_STATISTIC_CHECK_INTERVAL)
		}
	}

	override fun onStateChange(newState: Tunnel.State) {
		_vpnState.update {
			it.copy(status = TunnelState.from(newState))
		}
		serviceManager.requestTunnelTileUpdate()
	}

	override fun onStateChange(state: State) {
		_vpnState.update {
			it.copy(status = TunnelState.from(state))
		}
		serviceManager.requestTunnelTileUpdate()
	}

	companion object {
		const val STATS_START_DELAY = 1_000L
		const val VPN_STATISTIC_CHECK_INTERVAL = 1_000L
	}
}
