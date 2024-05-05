package com.zaneschepke.wireguardautotunnel.service.tunnel

import com.wireguard.android.backend.Backend
import com.wireguard.android.backend.BackendException
import com.wireguard.android.backend.Tunnel.State
import com.zaneschepke.wireguardautotunnel.WireGuardAutoTunnel
import com.zaneschepke.wireguardautotunnel.data.domain.TunnelConfig
import com.zaneschepke.wireguardautotunnel.data.repository.AppDataRepository
import com.zaneschepke.wireguardautotunnel.module.Kernel
import com.zaneschepke.wireguardautotunnel.module.Userspace
import com.zaneschepke.wireguardautotunnel.service.tunnel.statistics.AmneziaStatistics
import com.zaneschepke.wireguardautotunnel.service.tunnel.statistics.TunnelStatistics
import com.zaneschepke.wireguardautotunnel.service.tunnel.statistics.WireGuardStatistics
import com.zaneschepke.wireguardautotunnel.util.Constants
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.amnezia.awg.backend.Tunnel
import org.amnezia.awg.config.Config
import timber.log.Timber
import javax.inject.Inject

class WireGuardTunnel
@Inject
constructor(
    private val userspaceAmneziaBackend : org.amnezia.awg.backend.Backend,
    @Userspace private val userspaceBackend: Backend,
    @Kernel private val kernelBackend: Backend,
    private val appDataRepository: AppDataRepository,
) : VpnService {
    private val _vpnState = MutableStateFlow(VpnState())
    override val vpnState: StateFlow<VpnState> = _vpnState.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO)

    private var statsJob: Job? = null

    private var backend: Backend = userspaceBackend

    private var backendIsWgUserspace = true

    private var backendIsAmneziaUserspace = false

    init {
        scope.launch {
            appDataRepository.settings.getSettingsFlow().collect {
                if (it.isKernelEnabled && (backendIsWgUserspace || backendIsAmneziaUserspace)) {
                    Timber.d("Setting kernel backend")
                    backend = kernelBackend
                    backendIsWgUserspace = false
                    backendIsAmneziaUserspace = false
                } else if (!it.isKernelEnabled && !it.isAmneziaEnabled && !backendIsWgUserspace) {
                    Timber.d("Setting WireGuard userspace backend")
                    backend = userspaceBackend
                    backendIsWgUserspace = true
                    backendIsAmneziaUserspace = false
                } else if (it.isAmneziaEnabled && !backendIsAmneziaUserspace) {
                    Timber.d("Setting Amnezia userspace backend")
                    backendIsAmneziaUserspace = true
                    backendIsWgUserspace = false
                }
            }
        }
    }

    private fun setState(tunnelConfig: TunnelConfig?, tunnelState: TunnelState) : TunnelState {
        return if(backendIsAmneziaUserspace) {
            Timber.i("Using Amnezia backend")
            val config = tunnelConfig?.let {
                if(it.amQuick != "") TunnelConfig.configFromAmQuick(it.amQuick) else {
                    Timber.w("Using backwards compatible wg config, amnezia specific config not found.")
                    TunnelConfig.configFromAmQuick(it.wgQuick)
                }
            }
            val state = userspaceAmneziaBackend.setState(this, tunnelState.toAmState(), config)
            TunnelState.from(state)
        } else {
            Timber.i("Using Wg backend")
            val wgConfig = tunnelConfig?.let { TunnelConfig.configFromWgQuick(it.wgQuick) }
            val state = backend.setState(
                this,
                tunnelState.toWgState(),
                wgConfig,
            )
            TunnelState.from(state)
        }
    }

    override suspend fun startTunnel(tunnelConfig: TunnelConfig?): TunnelState {
        return try {
            //TODO we need better error handling here
            val config = tunnelConfig ?: appDataRepository.getPrimaryOrFirstTunnel()
            if (config != null) {
                emitTunnelConfig(config)
                setState(config, TunnelState.UP)
            } else throw Exception("No tunnels")
        } catch (e: BackendException) {
            Timber.e("Failed to start tunnel with error: ${e.message}")
            TunnelState.from(State.DOWN)
        }
    }

    private fun emitTunnelState(state : TunnelState) {
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

    private suspend fun emitTunnelConfig(tunnelConfig: TunnelConfig?) {
        _vpnState.emit(
            _vpnState.value.copy(
                tunnelConfig = tunnelConfig,
            ),
        )
    }

    override suspend fun stopTunnel() {
        try {
            if (getState() == TunnelState.UP) {
                val state = setState(null, TunnelState.DOWN)
                emitTunnelState(state)
            }
        } catch (e: BackendException) {
            Timber.e("Failed to stop wireguard tunnel with error: ${e.message}")
        } catch (e: org.amnezia.awg.backend.BackendException) {
            Timber.e("Failed to stop amnezia tunnel with error: ${e.message}")
        }
    }

    override fun getState(): TunnelState {
        return if(backendIsAmneziaUserspace) TunnelState.from(userspaceAmneziaBackend.getState(this))
        else TunnelState.from(backend.getState(this))
    }

    override fun getName(): String {
        return _vpnState.value.tunnelConfig?.name ?: ""
    }


    override fun onStateChange(newState: Tunnel.State) {
        handleStateChange(TunnelState.from(newState))
    }

    private fun handleStateChange(state: TunnelState) {
        val tunnel = this
        emitTunnelState(state)
        WireGuardAutoTunnel.requestTunnelTileServiceStateUpdate(WireGuardAutoTunnel.instance)
        if (state == TunnelState.UP) {
            statsJob =
                scope.launch {
                    while (true) {
                        if(backendIsAmneziaUserspace) {
                            emitBackendStatistics(AmneziaStatistics(userspaceAmneziaBackend.getStatistics(tunnel)))
                        } else {
                            emitBackendStatistics(WireGuardStatistics(backend.getStatistics(tunnel)))
                        }
                        delay(Constants.VPN_STATISTIC_CHECK_INTERVAL)
                    }
                }
        }
        if (state == TunnelState.DOWN) {
            try {
                statsJob?.cancel()
            } catch (e : CancellationException) {
                Timber.i("Stats job cancelled")
            }
        }
    }

    override fun onStateChange(state: State) {
        handleStateChange(TunnelState.from(state))
    }
}
