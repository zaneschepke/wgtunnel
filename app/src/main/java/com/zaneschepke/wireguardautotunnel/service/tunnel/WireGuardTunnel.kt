package com.zaneschepke.wireguardautotunnel.service.tunnel

import com.wireguard.android.backend.Backend
import com.wireguard.android.backend.BackendException
import com.wireguard.android.backend.Statistics
import com.wireguard.android.backend.Tunnel
import com.wireguard.config.Config
import com.wireguard.crypto.Key
import com.zaneschepke.wireguardautotunnel.Constants
import com.zaneschepke.wireguardautotunnel.module.Kernel
import com.zaneschepke.wireguardautotunnel.module.Userspace
import com.zaneschepke.wireguardautotunnel.repository.SettingsDoa
import com.zaneschepke.wireguardautotunnel.repository.model.TunnelConfig
import com.zaneschepke.wireguardautotunnel.util.NumberUtils
import javax.inject.Inject
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

class WireGuardTunnel
@Inject
constructor(
    @Userspace private val userspaceBackend: Backend,
    @Kernel private val kernelBackend: Backend,
    private val settingsRepo: SettingsDoa
) : VpnService {
    private val _tunnelName = MutableStateFlow("")
    override val tunnelName get() = _tunnelName.asStateFlow()

    private val _state =
        MutableSharedFlow<Tunnel.State>(
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
            replay = 1
        )

    private val _handshakeStatus =
        MutableSharedFlow<HandshakeStatus>(
            replay = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )
    override val state get() = _state.asSharedFlow()

    private val _statistics = MutableSharedFlow<Statistics>(replay = 1)
    override val statistics get() = _statistics.asSharedFlow()

    private val _lastHandshake = MutableSharedFlow<Map<Key, Long>>(replay = 1)
    override val lastHandshake get() = _lastHandshake.asSharedFlow()

    override val handshakeStatus: SharedFlow<HandshakeStatus>
        get() = _handshakeStatus.asSharedFlow()

    private val scope = CoroutineScope(Dispatchers.IO)

    private lateinit var statsJob: Job

    private var config: Config? = null

    private var backend: Backend = userspaceBackend

    private var backendIsUserspace = true

    init {
        scope.launch {
            settingsRepo.getAllFlow().collect {
                val settings = it.first()
                if (settings.isKernelEnabled && backendIsUserspace) {
                    Timber.d("Setting kernel backend")
                    backend = kernelBackend
                    backendIsUserspace = false
                } else if (!settings.isKernelEnabled && !backendIsUserspace) {
                    Timber.d("Setting userspace backend")
                    backend = userspaceBackend
                    backendIsUserspace = true
                }
            }
        }
    }

    override suspend fun startTunnel(tunnelConfig: TunnelConfig): Tunnel.State {
        return try {
            stopTunnelOnConfigChange(tunnelConfig)
            emitTunnelName(tunnelConfig.name)
            config = TunnelConfig.configFromQuick(tunnelConfig.wgQuick)
            val state =
                backend.setState(
                    this,
                    Tunnel.State.UP,
                    config
                )
            _state.emit(state)
            state
        } catch (e: Exception) {
            Timber.e("Failed to start tunnel with error: ${e.message}")
            Tunnel.State.DOWN
        }
    }

    private suspend fun emitTunnelName(name: String) {
        _tunnelName.emit(name)
    }

    private suspend fun stopTunnelOnConfigChange(tunnelConfig: TunnelConfig) {
        if (getState() == Tunnel.State.UP && _tunnelName.value != tunnelConfig.name) {
            stopTunnel()
        }
    }

    override fun getName(): String {
        return _tunnelName.value
    }

    override suspend fun stopTunnel() {
        try {
            if (getState() == Tunnel.State.UP) {
                val state = backend.setState(this, Tunnel.State.DOWN, null)
                _state.emit(state)
            }
        } catch (e: BackendException) {
            Timber.e("Failed to stop tunnel with error: ${e.message}")
        }
    }

    override fun getState(): Tunnel.State {
        return backend.getState(this)
    }

    override fun onStateChange(state: Tunnel.State) {
        val tunnel = this
        _state.tryEmit(state)
        if (state == Tunnel.State.UP) {
            statsJob =
                scope.launch {
                    val handshakeMap = HashMap<Key, Long>()
                    var neverHadHandshakeCounter = 0
                    while (true) {
                        val statistics = backend.getStatistics(tunnel)
                        _statistics.emit(statistics)
                        statistics.peers().forEach { key ->
                            val handshakeEpoch =
                                statistics.peer(key)?.latestHandshakeEpochMillis ?: 0L
                            handshakeMap[key] = handshakeEpoch
                            if (handshakeEpoch == 0L) {
                                if (neverHadHandshakeCounter >= HandshakeStatus.NEVER_CONNECTED_TO_UNHEALTHY_TIME_LIMIT_SEC) {
                                    _handshakeStatus.emit(HandshakeStatus.NEVER_CONNECTED)
                                } else {
                                    _handshakeStatus.emit(HandshakeStatus.NOT_STARTED)
                                }
                                if (neverHadHandshakeCounter <= HandshakeStatus.NEVER_CONNECTED_TO_UNHEALTHY_TIME_LIMIT_SEC) {
                                    neverHadHandshakeCounter += (1 * Constants.VPN_STATISTIC_CHECK_INTERVAL / 1000).toInt()
                                }
                                return@forEach
                            }
                            // TODO one day make each peer have their own dedicated status
                            val lastHandshake = NumberUtils.getSecondsBetweenTimestampAndNow(
                                handshakeEpoch
                            )
                            if (lastHandshake != null) {
                                if (lastHandshake >= HandshakeStatus.STALE_TIME_LIMIT_SEC) {
                                    _handshakeStatus.emit(HandshakeStatus.STALE)
                                } else {
                                    _handshakeStatus.emit(HandshakeStatus.HEALTHY)
                                }
                            }
                        }
                        _lastHandshake.emit(handshakeMap)
                        delay(Constants.VPN_STATISTIC_CHECK_INTERVAL)
                    }
                }
        }
        if (state == Tunnel.State.DOWN) {
            if (this::statsJob.isInitialized) {
                statsJob.cancel()
            }
            _handshakeStatus.tryEmit(HandshakeStatus.NOT_STARTED)
            _lastHandshake.tryEmit(emptyMap())
        }
    }
}
