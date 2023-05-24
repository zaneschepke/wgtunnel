package com.zaneschepke.wireguardautotunnel.service.tunnel

import com.wireguard.android.backend.Backend
import com.wireguard.android.backend.BackendException
import com.wireguard.android.backend.Tunnel
import com.zaneschepke.wireguardautotunnel.service.tunnel.model.TunnelConfig
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject


class WireGuardTunnel @Inject constructor(private val backend : Backend) : VpnService {

    private val _tunnelName = MutableStateFlow("")
    override val tunnelName get() = _tunnelName.asStateFlow()
    private val _state = MutableSharedFlow<Tunnel.State>(
        replay = 1,
        onBufferOverflow = BufferOverflow.SUSPEND,
        extraBufferCapacity = 1)
    override val state get() = _state.asSharedFlow()

    override suspend fun startTunnel(tunnelConfig: TunnelConfig) : Tunnel.State{
        try {
            if(getState() == Tunnel.State.UP && _tunnelName.value != tunnelConfig.name) {
                stopTunnel()
            }
            _tunnelName.emit(tunnelConfig.name)
            val config = TunnelConfig.configFromQuick(tunnelConfig.wgQuick)
            val state = backend.setState(
                this, Tunnel.State.UP, config)
            _state.emit(state)
            return state;
        } catch (e : Exception) {
            Timber.e("Failed to start tunnel with error: ${e.message}")
            return Tunnel.State.DOWN
        }
    }

    override fun getName(): String {
        return _tunnelName.value
    }

    override suspend  fun stopTunnel() {
        try {
            if(getState() == Tunnel.State.UP) {
                val state = backend.setState(this, Tunnel.State.DOWN, null)
                _state.emit(state)
            }
        } catch (e : BackendException) {
            Timber.e("Failed to stop tunnel with error: ${e.message}")
        }
    }

    override fun getState(): Tunnel.State {
        return backend.getState(this)
    }

    override fun onStateChange(state : Tunnel.State) {
        _state.tryEmit(state)
    }
}