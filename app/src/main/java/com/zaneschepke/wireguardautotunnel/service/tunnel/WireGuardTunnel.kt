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

	override val runningTunnelNames: Set<String> = wgBackend().runningTunnelNames

	private var statsJob: Job? = null

	private var backendIsWgUserspace = true

	private var backendIsAmneziaUserspace = false

	init {
		applicationScope.launch(ioDispatcher) {
			appDataRepository.settings.getSettingsFlow().collect {
				if (it.isKernelEnabled && (backendIsWgUserspace || backendIsAmneziaUserspace)) {
					Timber.i("Setting kernel backend")
					backendIsWgUserspace = false
					backendIsAmneziaUserspace = false
				} else if (!it.isKernelEnabled && !it.isAmneziaEnabled && !backendIsWgUserspace) {
					Timber.i("Setting WireGuard userspace backend")
					backendIsWgUserspace = true
					backendIsAmneziaUserspace = false
				} else if (it.isAmneziaEnabled && !backendIsAmneziaUserspace) {
					Timber.i("Setting Amnezia userspace backend")
					backendIsAmneziaUserspace = true
					backendIsWgUserspace = false
				}
			}
		}
	}

	private suspend fun setState(tunnelConfig: TunnelConfig, tunnelState: TunnelState): Result<TunnelState> {
		return runCatching {
			if (backendIsAmneziaUserspace) {
				setStateAmnezia(tunnelConfig, tunnelState)
			} else {
				setStateWg(tunnelConfig, tunnelState)
			}
		}.onFailure {
			Timber.e(it)
		}
	}

	private suspend fun setStateAmnezia(tunnelConfig: TunnelConfig, tunnelState: TunnelState): TunnelState {
		return withContext(ioDispatcher) {
			Timber.i("Using Amnezia backend")
			val config = if (tunnelConfig.amQuick != "") {
				TunnelConfig.configFromAmQuick(tunnelConfig.amQuick)
			} else {
				Timber.w(
					"Using backwards compatible wg config, amnezia specific config not found.",
				)
				TunnelConfig.configFromAmQuick(tunnelConfig.wgQuick)
			}
			val state = amneziaBackend.get().setState(this@WireGuardTunnel, tunnelState.toAmState(), config)
			TunnelState.from(state)
		}
	}

	private suspend fun setStateWg(tunnelConfig: TunnelConfig, tunnelState: TunnelState): TunnelState {
		return withContext(ioDispatcher) {
			Timber.i("Using Wg backend")
			val wgConfig = TunnelConfig.configFromWgQuick(tunnelConfig.wgQuick)
			val state =
				wgBackend().setState(
					this@WireGuardTunnel,
					tunnelState.toWgState(),
					wgConfig,
				)
			TunnelState.from(state)
		}
	}

	override suspend fun startTunnel(tunnelConfig: TunnelConfig): Result<TunnelState> {
		return withContext(ioDispatcher) {
			if (_vpnState.value.status == TunnelState.UP) vpnState.value.tunnelConfig?.let { stopTunnel(it) }
			emitTunnelConfig(tunnelConfig)
			setState(tunnelConfig, TunnelState.UP).onSuccess {
				emitTunnelState(it)
				appDataRepository.tunnels.save(tunnelConfig.copy(isActive = it == TunnelState.UP))
				appDataRepository.appState.setActiveTunnelId(tunnelConfig.id)
				WireGuardAutoTunnel.instance.requestTunnelTileServiceStateUpdate()
			}
		}
	}

	private fun wgBackend(): Backend {
		return when {
			backendIsWgUserspace -> userspaceBackend.get()
			!backendIsWgUserspace && !backendIsAmneziaUserspace -> kernelBackend.get()
			else -> userspaceBackend.get()
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

	override suspend fun stopTunnel(tunnelConfig: TunnelConfig): Result<TunnelState> {
		return withContext(ioDispatcher) {
			setState(tunnelConfig, TunnelState.DOWN).onSuccess {
				emitTunnelState(it)
				appDataRepository.tunnels.save(tunnelConfig.copy(isActive = false))
				resetBackendStatistics()
				WireGuardAutoTunnel.instance.requestTunnelTileServiceStateUpdate()
			}.onFailure {
				Timber.e(it)
			}
		}
	}

	override fun getState(): TunnelState {
		return if (backendIsAmneziaUserspace) {
			TunnelState.from(
				amneziaBackend.get().getState(this),
			)
		} else {
			TunnelState.from(wgBackend().getState(this))
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
		while (true) {
			if (backendIsAmneziaUserspace) {
				emitBackendStatistics(
					AmneziaStatistics(
						amneziaBackend.get().getStatistics(this@WireGuardTunnel),
					),
				)
			} else {
				emitBackendStatistics(
					WireGuardStatistics(wgBackend().getStatistics(this@WireGuardTunnel)),
				)
			}
			delay(Constants.VPN_STATISTIC_CHECK_INTERVAL)
		}
	}

	override fun onStateChange(state: State) {
		handleStateChange(TunnelState.from(state))
	}
}
