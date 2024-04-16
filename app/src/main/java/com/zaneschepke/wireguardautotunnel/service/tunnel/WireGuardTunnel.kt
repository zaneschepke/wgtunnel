package com.zaneschepke.wireguardautotunnel.service.tunnel

import com.wireguard.android.backend.Backend
import com.wireguard.android.backend.BackendException
import com.wireguard.android.backend.Statistics
import com.wireguard.android.backend.Tunnel.State
import com.zaneschepke.wireguardautotunnel.WireGuardAutoTunnel
import com.zaneschepke.wireguardautotunnel.data.model.TunnelConfig
import com.zaneschepke.wireguardautotunnel.data.repository.AppDataRepository
import com.zaneschepke.wireguardautotunnel.module.Kernel
import com.zaneschepke.wireguardautotunnel.module.Userspace
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
import timber.log.Timber
import javax.inject.Inject

class WireGuardTunnel
@Inject
constructor(
    @Userspace private val userspaceBackend: Backend,
    @Kernel private val kernelBackend: Backend,
    private val appDataRepository: AppDataRepository,
) : VpnService {
    private val _vpnState = MutableStateFlow(VpnState())
    override val vpnState: StateFlow<VpnState> = _vpnState.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO)

    private var statsJob: Job? = null

    private var backend: Backend = userspaceBackend

    private var backendIsUserspace = true

    init {
        scope.launch {
            appDataRepository.settings.getSettingsFlow().collect {
                if (it.isKernelEnabled && backendIsUserspace) {
                    Timber.d("Setting kernel backend")
                    backend = kernelBackend
                    backendIsUserspace = false
                } else if (!it.isKernelEnabled && !backendIsUserspace) {
                    Timber.d("Setting userspace backend")
                    backend = userspaceBackend
                    backendIsUserspace = true
                }
            }
        }
    }

    override suspend fun startTunnel(tunnelConfig: TunnelConfig?): State {
        return try {
            //TODO we need better error handling here
            val config = tunnelConfig ?: appDataRepository.getPrimaryOrFirstTunnel()
            if (config != null) {
                emitTunnelConfig(config)
                val wgConfig = TunnelConfig.configFromQuick(config.wgQuick)
                val state =
                    backend.setState(
                        this,
                        State.UP,
                        wgConfig,
                    )
                state
            } else throw Exception("No tunnels")
        } catch (e: BackendException) {
            Timber.e("Failed to start tunnel with error: ${e.message}")
            State.DOWN
        }
    }

    private fun emitTunnelState(state: State) {
        _vpnState.tryEmit(
            _vpnState.value.copy(
                status = state,
            ),
        )
    }

    private fun emitBackendStatistics(statistics: Statistics) {
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
            if (getState() == State.UP) {
                val state = backend.setState(this, State.DOWN, null)
                emitTunnelState(state)
            }
        } catch (e: BackendException) {
            Timber.e("Failed to stop tunnel with error: ${e.message}")
        }
    }

    override fun getState(): State {
        return backend.getState(this)
    }

    override fun getName(): String {
        return _vpnState.value.tunnelConfig?.name ?: ""
    }

    override fun onStateChange(state: State) {
        val tunnel = this
        emitTunnelState(state)
        WireGuardAutoTunnel.requestTunnelTileServiceStateUpdate(WireGuardAutoTunnel.instance)
        if (state == State.UP) {
            statsJob =
                scope.launch {
                    while (true) {
                        val statistics = backend.getStatistics(tunnel)
                        emitBackendStatistics(statistics)
                        delay(Constants.VPN_STATISTIC_CHECK_INTERVAL)
                    }
                }
        }
        if (state == State.DOWN) {
            try {
                statsJob?.cancel()
            } catch (e : CancellationException) {
                Timber.i("Stats job cancelled")
            }
        }
    }
}
