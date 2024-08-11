package com.zaneschepke.wireguardautotunnel.service.tunnel

import com.wireguard.android.backend.Backend
import com.wireguard.android.backend.Tunnel.State
import com.zaneschepke.wireguardautotunnel.WireGuardAutoTunnel
import com.zaneschepke.wireguardautotunnel.data.domain.TunnelConfig
import com.zaneschepke.wireguardautotunnel.data.repository.AppDataRepository
import com.zaneschepke.wireguardautotunnel.module.ApplicationScope
import com.zaneschepke.wireguardautotunnel.module.IoDispatcher
import com.zaneschepke.wireguardautotunnel.module.Kernel
import com.zaneschepke.wireguardautotunnel.module.Userspace
import com.zaneschepke.wireguardautotunnel.service.tunnel.statistics.AmneziaStatistics
import com.zaneschepke.wireguardautotunnel.service.tunnel.statistics.TunnelStatistics
import com.zaneschepke.wireguardautotunnel.service.tunnel.statistics.WireGuardStatistics
import com.zaneschepke.wireguardautotunnel.util.Constants
import com.zaneschepke.wireguardautotunnel.util.extensions.requestTunnelTileServiceStateUpdate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
	@Userspace private val userspaceBackend: Provider<Backend>,
	@Kernel private val kernelBackend: Provider<Backend>,
	private val appDataRepository: AppDataRepository,
	@ApplicationScope private val applicationScope: CoroutineScope,
	@IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : TunnelService {
	private val _vpnState = MutableStateFlow(VpnState())
	override val vpnState: StateFlow<VpnState> = _vpnState.asStateFlow()

	override suspend fun runningTunnelNames(): Set<String> {
		return when (val backend = backend()) {
			is Backend -> backend.runningTunnelNames
			is org.amnezia.awg.backend.Backend -> backend.runningTunnelNames
			else -> emptySet()
		}
	}

	private var statsJob: Job? = null

	private suspend fun setState(tunnelConfig: TunnelConfig, tunnelState: TunnelState): Result<TunnelState> {
		return runCatching {
			when (val backend = backend()) {
				is Backend -> backend.setState(this, tunnelState.toWgState(), TunnelConfig.configFromWgQuick(tunnelConfig.wgQuick)).let { TunnelState.from(it) }
				is org.amnezia.awg.backend.Backend -> backend.setState(this, tunnelState.toAmState(), TunnelConfig.configFromAmQuick(tunnelConfig.amQuick)).let {
					TunnelState.from(it)
				}
				else -> throw NotImplementedError()
			}
		}.onFailure {
			Timber.e(it)
		}
	}

	private suspend fun backend(): Any {
		val settings = appDataRepository.settings.getSettings()
		if (settings.isKernelEnabled) return kernelBackend.get()
		if (settings.isAmneziaEnabled) return amneziaBackend.get()
		return userspaceBackend.get()
	}

	override suspend fun startTunnel(tunnelConfig: TunnelConfig): Result<TunnelState> {
		return withContext(ioDispatcher) {
			if (_vpnState.value.status == TunnelState.UP) vpnState.value.tunnelConfig?.let { stopTunnel(it) }
			appDataRepository.tunnels.save(tunnelConfig.copy(isActive = true))
			appDataRepository.appState.setLastActiveTunnelId(tunnelConfig.id)
			emitTunnelConfig(tunnelConfig)
			setState(tunnelConfig, TunnelState.UP).onSuccess {
				emitTunnelState(it)
				WireGuardAutoTunnel.instance.requestTunnelTileServiceStateUpdate()
			}.onFailure {
				appDataRepository.tunnels.save(tunnelConfig.copy(isActive = false))
				WireGuardAutoTunnel.instance.requestTunnelTileServiceStateUpdate()
			}
		}
	}

	override suspend fun stopTunnel(tunnelConfig: TunnelConfig): Result<TunnelState> {
		return withContext(ioDispatcher) {
			appDataRepository.tunnels.save(tunnelConfig.copy(isActive = false))
			setState(tunnelConfig, TunnelState.DOWN).onSuccess {
				emitTunnelState(it)
				resetBackendStatistics()
				WireGuardAutoTunnel.instance.requestTunnelTileServiceStateUpdate()
			}.onFailure {
				Timber.e(it)
				appDataRepository.tunnels.save(tunnelConfig.copy(isActive = true))
				WireGuardAutoTunnel.instance.requestTunnelTileServiceStateUpdate()
			}
		}
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

	private fun emitTunnelConfig(tunnelConfig: TunnelConfig?) {
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

	override fun getName(): String {
		return _vpnState.value.tunnelConfig?.name ?: ""
	}

	override fun onStateChange(newState: Tunnel.State) {
		handleStateChange(TunnelState.from(newState))
	}

	private fun handleStateChange(state: TunnelState) {
		emitTunnelState(state)
		WireGuardAutoTunnel.instance.requestTunnelTileServiceStateUpdate()
		if (state == TunnelState.UP) {
			statsJob = startTunnelStatisticsJob()
		}
		if (state == TunnelState.DOWN) {
			try {
				statsJob?.cancel()
			} catch (e: CancellationException) {
				Timber.i("Stats job cancelled")
			}
		}
	}

	private fun startTunnelStatisticsJob() = applicationScope.launch(ioDispatcher) {
		val backend = backend()
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
			delay(Constants.VPN_STATISTIC_CHECK_INTERVAL)
		}
	}

	override fun onStateChange(state: State) {
		handleStateChange(TunnelState.from(state))
	}
}
