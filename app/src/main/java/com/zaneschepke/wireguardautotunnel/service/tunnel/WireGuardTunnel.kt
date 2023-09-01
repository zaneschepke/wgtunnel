package com.zaneschepke.wireguardautotunnel.service.tunnel

import com.wireguard.android.backend.Backend
import com.wireguard.android.backend.BackendException
import com.wireguard.android.backend.Statistics
import com.wireguard.android.backend.Tunnel
import com.wireguard.crypto.Key
import com.zaneschepke.wireguardautotunnel.Constants
import com.zaneschepke.wireguardautotunnel.service.tunnel.model.TunnelConfig
import com.zaneschepke.wireguardautotunnel.util.NumberUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject


class WireGuardTunnel @Inject constructor(private val backend : Backend,
) : VpnService {

    private val _tunnelName = MutableStateFlow("")
    override val tunnelName get() = _tunnelName.asStateFlow()

    private val _state = MutableSharedFlow<Tunnel.State>(
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
        replay = 1)

    private val _handshakeStatus = MutableSharedFlow<HandshakeStatus>(replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST)
    override val state get() = _state.asSharedFlow()

    private val _statistics = MutableSharedFlow<Statistics>(replay = 1)
    override val statistics get() = _statistics.asSharedFlow()

    private val _lastHandshake = MutableSharedFlow<Map<Key, Long>>(replay = 1)
    override val lastHandshake get() = _lastHandshake.asSharedFlow()

    override val handshakeStatus: SharedFlow<HandshakeStatus>
        get() = _handshakeStatus.asSharedFlow()

    private lateinit var statsJob : Job


    override suspend fun startTunnel(tunnelConfig: TunnelConfig) : Tunnel.State{
        return try {
            if(getState() == Tunnel.State.UP && _tunnelName.value != tunnelConfig.name) {
                stopTunnel()
            }
            _tunnelName.emit(tunnelConfig.name)
            val config = TunnelConfig.configFromQuick(tunnelConfig.wgQuick)
            val state = backend.setState(
                this, Tunnel.State.UP, config)
            _state.emit(state)
            state;
        } catch (e : Exception) {
            Timber.e("Failed to start tunnel with error: ${e.message}")
            Tunnel.State.DOWN
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
        val tunnel = this;
        _state.tryEmit(state)
        if(state == Tunnel.State.UP) {
            statsJob = CoroutineScope(Dispatchers.IO).launch {
                val handshakeMap = HashMap<Key, Long>()
                var neverHadHandshakeCounter = 0
                while (true) {
                    val statistics = backend.getStatistics(tunnel)
                    _statistics.emit(statistics)
                    statistics.peers().forEach {
                        val handshakeEpoch = statistics.peer(it)?.latestHandshakeEpochMillis ?: 0L
                        handshakeMap[it] = handshakeEpoch
                        if(handshakeEpoch == 0L) {
                            if(neverHadHandshakeCounter >= HandshakeStatus.NEVER_CONNECTED_TO_UNHEALTHY_TIME_LIMIT_SEC) {
                                _handshakeStatus.emit(HandshakeStatus.NEVER_CONNECTED)
                            } else {
                                _handshakeStatus.emit(HandshakeStatus.NOT_STARTED)
                            }
                            if(neverHadHandshakeCounter <= HandshakeStatus.NEVER_CONNECTED_TO_UNHEALTHY_TIME_LIMIT_SEC) {
                                neverHadHandshakeCounter += 10
                            }
                            return@forEach
                        }
                        if(NumberUtils.getSecondsBetweenTimestampAndNow(handshakeEpoch) >= HandshakeStatus.UNHEALTHY_TIME_LIMIT_SEC) {
                            _handshakeStatus.emit(HandshakeStatus.UNHEALTHY)
                        } else {
                            _handshakeStatus.emit(HandshakeStatus.HEALTHY)
                        }
                    }
                    _lastHandshake.emit(handshakeMap)
                    delay(Constants.VPN_STATISTIC_CHECK_INTERVAL)
                }
            }
        }
        if(state == Tunnel.State.DOWN) {
            if(this::statsJob.isInitialized) {
                statsJob.cancel()
            }
            _handshakeStatus.tryEmit(HandshakeStatus.NOT_STARTED)
            _lastHandshake.tryEmit(emptyMap())
        }
    }
}