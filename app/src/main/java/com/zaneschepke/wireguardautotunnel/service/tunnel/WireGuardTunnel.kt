package com.zaneschepke.wireguardautotunnel.service.tunnel

import com.wireguard.android.backend.Backend
import com.wireguard.android.backend.Tunnel.State
import com.zaneschepke.wireguardautotunnel.data.domain.TunnelConfig
import com.zaneschepke.wireguardautotunnel.data.repository.AppDataRepository
import com.zaneschepke.wireguardautotunnel.data.repository.TunnelConfigRepository
import com.zaneschepke.wireguardautotunnel.module.ApplicationScope
import com.zaneschepke.wireguardautotunnel.module.IoDispatcher
import com.zaneschepke.wireguardautotunnel.module.Kernel
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
import kotlinx.coroutines.launch
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
) : TunnelService {

	private val _vpnState = MutableStateFlow(VpnState())
	override val vpnState: StateFlow<VpnState> = _vpnState.combine(
		tunnelConfigRepository.getTunnelConfigsFlow(),
	) {
			vpnState, tunnels ->
		vpnState.copy(
			tunnelConfig = tunnels.firstOrNull { it.id == vpnState.tunnelConfig?.id },
		)
	}.stateIn(applicationScope, SharingStarted.Lazily, VpnState())

	private var statsJob: Job? = null

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

	override suspend fun startTunnel(tunnelConfig: TunnelConfig): Result<TunnelState> {
		return withContext(ioDispatcher) {
			onBeforeStart(tunnelConfig)
			setState(tunnelConfig, TunnelState.UP).onSuccess {
				emitTunnelState(it)
			}.onFailure {
				Timber.e(it)
				onStartFailed()
			}
		}
	}

	override suspend fun stopTunnel(tunnelConfig: TunnelConfig): Result<TunnelState> {
		return withContext(ioDispatcher) {
			onBeforeStop(tunnelConfig)
			setState(tunnelConfig, TunnelState.DOWN).onSuccess {
				emitTunnelState(it)
			}.onFailure {
				Timber.e(it)
				onStopFailed()
			}
		}
	}

	// use this when we just want to bounce tunnel and not change tunnelConfig active state
	override suspend fun bounceTunnel(tunnelConfig: TunnelConfig): Result<TunnelState> {
		toggleTunnel(tunnelConfig)
		delay(VPN_RESTART_DELAY)
		return toggleTunnel(tunnelConfig)
	}

	private suspend fun toggleTunnel(tunnelConfig: TunnelConfig): Result<TunnelState> {
		return withContext(ioDispatcher) {
			setState(tunnelConfig, TunnelState.TOGGLE).onSuccess {
				emitTunnelState(it)
				resetBackendStatistics()
			}.onFailure {
				Timber.e(it)
			}
		}
	}

	private suspend fun onStopFailed() {
		_vpnState.value.tunnelConfig?.let {
			appDataRepository.tunnels.save(it.copy(isActive = true))
		}
	}

	private suspend fun onStartFailed() {
		_vpnState.value.tunnelConfig?.let {
			appDataRepository.tunnels.save(it.copy(isActive = false))
		}
		cancelStatsJob()
		resetBackendStatistics()
	}

	private suspend fun onBeforeStart(tunnelConfig: TunnelConfig) {
		if (_vpnState.value.status == TunnelState.UP) vpnState.value.tunnelConfig?.let { stopTunnel(it) }
		resetBackendStatistics()
		appDataRepository.tunnels.save(tunnelConfig.copy(isActive = true))
		emitVpnStateConfig(tunnelConfig)
		startStatsJob()
	}

	private suspend fun onBeforeStop(tunnelConfig: TunnelConfig) {
		cancelStatsJob()
		resetBackendStatistics()
		appDataRepository.tunnels.save(tunnelConfig.copy(isActive = false))
	}

	private fun emitTunnelState(state: TunnelState) {
		_vpnState.tryEmit(
			_vpnState.value.copy(
				status = state,
			),
		)
	}

	private fun emitBackendStatistics(statistics: TunnelStatistics) {
		_vpnState.tryEmit(
			_vpnState.value.copy(
				statistics = statistics,
			),
		)
	}

	private fun emitVpnStateConfig(tunnelConfig: TunnelConfig) {
		_vpnState.tryEmit(
			_vpnState.value.copy(
				tunnelConfig = tunnelConfig,
			),
		)
	}

	private fun resetBackendStatistics() {
		_vpnState.tryEmit(
			_vpnState.value.copy(
				statistics = null,
			),
		)
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
		emitTunnelState(TunnelState.from(newState))
	}

	override fun onStateChange(state: State) {
		emitTunnelState(TunnelState.from(state))
	}

	companion object {
		const val STATS_START_DELAY = 5_000L
		const val VPN_STATISTIC_CHECK_INTERVAL = 1_000L
		const val VPN_RESTART_DELAY = 1_000L
	}
}
