package com.zaneschepke.wireguardautotunnel.core.tunnel

import com.wireguard.android.backend.Tunnel
import com.zaneschepke.wireguardautotunnel.core.service.ServiceManager
import com.zaneschepke.wireguardautotunnel.di.ApplicationScope
import com.zaneschepke.wireguardautotunnel.domain.enums.BackendError
import com.zaneschepke.wireguardautotunnel.domain.enums.TunnelStatus
import com.zaneschepke.wireguardautotunnel.domain.model.TunnelConf
import com.zaneschepke.wireguardautotunnel.domain.repository.AppDataRepository
import com.zaneschepke.wireguardautotunnel.domain.state.TunnelState
import com.zaneschepke.wireguardautotunnel.domain.state.TunnelStatistics
import com.zaneschepke.wireguardautotunnel.util.extensions.asTunnelState
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

abstract class BaseTunnel(
    @ApplicationScope private val applicationScope: CoroutineScope,
    private val appDataRepository: AppDataRepository,
    private val serviceManager: ServiceManager,
) : TunnelProvider {

    private val _errorEvents =
        MutableSharedFlow<Pair<TunnelConf, BackendError>>(replay = 0, extraBufferCapacity = 1)
    override val errorEvents = _errorEvents.asSharedFlow()

    private val activeTuns = MutableStateFlow<Map<TunnelConf, TunnelState>>(emptyMap())
    private val tunThreads = ConcurrentHashMap<Int, Thread>()
    override val activeTunnels = activeTuns.asStateFlow()

    private val tunMutex = Mutex()
    private val tunStatusMutex = Mutex()
    private val bounceTunnelMutex = Mutex()

    override val bouncingTunnelIds = ConcurrentHashMap<Int, TunnelStatus.StopReason>()

    abstract suspend fun startBackend(tunnel: TunnelConf)

    abstract fun stopBackend(tunnel: TunnelConf)

    override fun hasVpnPermission(): Boolean {
        return serviceManager.hasVpnPermission()
    }

    protected suspend fun updateTunnelStatus(
        tunnelConf: TunnelConf,
        status: TunnelStatus? = null,
        stats: TunnelStatistics? = null,
    ) {
        tunStatusMutex.withLock {
            activeTuns.update { currentTuns ->
                val originalConf = currentTuns.getKeyById(tunnelConf.id) ?: tunnelConf
                val existingState = currentTuns.getValueById(tunnelConf.id) ?: TunnelState()
                val newState = status ?: existingState.status
                if (newState == TunnelStatus.Down) {
                    Timber.d("Removing tunnel ${tunnelConf.id} from activeTunnels as state is DOWN")
                    cleanUpTunThread(tunnelConf)
                    currentTuns - originalConf
                } else if (existingState.status == newState && stats == null) {
                    Timber.d("Skipping redundant state update for ${tunnelConf.id}: $newState")
                    currentTuns
                } else {
                    val updated =
                        existingState.copy(
                            status = newState,
                            statistics = stats ?: existingState.statistics,
                        )
                    currentTuns + (originalConf to updated)
                }
            }
        }
    }

    private suspend fun stopActiveTunnels() {
        activeTunnels.value.forEach { (config, state) ->
            if (state.status.isUpOrStarting()) {
                stopTunnel(config)
            }
        }
    }

    private fun configureTunnelCallbacks(tunnelConf: TunnelConf) {
        Timber.d("Configuring TunnelConf instance: ${tunnelConf.hashCode()}")
        tunnelConf.setStateChangeCallback { state ->
            applicationScope.launch {
                Timber.d(
                    "State change callback triggered for tunnel ${tunnelConf.id}: ${tunnelConf.tunName} with state $state at ${System.currentTimeMillis()}"
                )
                when (state) {
                    is Tunnel.State -> updateTunnelStatus(tunnelConf, state.asTunnelState())
                    is org.amnezia.awg.backend.Tunnel.State ->
                        updateTunnelStatus(tunnelConf, state.asTunnelState())
                }
                handleServiceStateOnChange()
            }
            serviceManager.updateTunnelTile()
        }
    }

    override suspend fun updateTunnelStatistics(tunnel: TunnelConf) {
        val stats = getStatistics(tunnel)
        updateTunnelStatus(tunnel, null, stats)
    }

    override suspend fun startTunnel(tunnelConf: TunnelConf) {
        if (activeTuns.exists(tunnelConf.id) || tunThreads.containsKey(tunnelConf.id)) return
        if (this@BaseTunnel is UserspaceTunnel) stopActiveTunnels()
        tunMutex.withLock {
            tunThreads[tunnelConf.id] = thread {
                try {
                    runBlocking {
                        Timber.d("Starting tunnel ${tunnelConf.id}...")
                        startTunnelInner(tunnelConf)
                        Timber.d("Started complete for tunnel ${tunnelConf.name}...")
                    }
                } catch (e: InterruptedException) {
                    Timber.w(
                        "Tunnel start has been interrupted as ${tunnelConf.name} failed to start"
                    )
                }
            }
        }
    }

    private suspend fun startTunnelInner(tunnelConf: TunnelConf) {
        configureTunnelCallbacks(tunnelConf)
        Timber.d("Starting  backend for tunnel ${tunnelConf.id}...")
        try {
            startBackend(tunnelConf)
            updateTunnelStatus(tunnelConf, TunnelStatus.Up)
            Timber.d("Started for tun ${tunnelConf.id}...")
            saveTunnelActiveState(tunnelConf, true)
            serviceManager.startTunnelForegroundService()
        } catch (e: BackendError) {
            Timber.e(e, "Failed to start backend for ${tunnelConf.name}")
            _errorEvents.emit(tunnelConf to e)
            updateTunnelStatus(tunnelConf, TunnelStatus.Down)
        }
    }

    private suspend fun saveTunnelActiveState(tunnelConf: TunnelConf, active: Boolean) {
        val tunnelCopy = tunnelConf.copyWithCallback(isActive = active)
        appDataRepository.tunnels.save(tunnelCopy)
    }

    override suspend fun stopTunnel(tunnelConf: TunnelConf?, reason: TunnelStatus.StopReason) {
        if (tunnelConf == null) return stopActiveTunnels()
        tunMutex.withLock {
            if (activeTuns.isStarting(tunnelConf.id))
                return handleStuckStartingTunnelShutdown(tunnelConf)
            updateTunnelStatus(tunnelConf, TunnelStatus.Stopping(reason))
            stopTunnelInner(tunnelConf)
        }
    }

    private suspend fun stopTunnelInner(tunnelConf: TunnelConf) {
        try {
            val tunnel = activeTuns.findTunnel(tunnelConf.id) ?: return
            stopBackend(tunnel)
            saveTunnelActiveState(tunnelConf, false)
            removeActiveTunnel(tunnel)
        } catch (e: BackendError) {
            Timber.e(e, "Failed to stop tunnel ${tunnelConf.id}")
            _errorEvents.emit(tunnelConf to e)
            updateTunnelStatus(tunnelConf, TunnelStatus.Down)
        }
    }

    private fun handleServiceStateOnChange() {
        if (activeTuns.value.isEmpty() && bouncingTunnelIds.isEmpty())
            serviceManager.stopTunnelForegroundService()
    }

    private suspend fun handleStuckStartingTunnelShutdown(tunnel: TunnelConf) {
        Timber.d("Stuck in starting state so shutting down tunnel thread for tunnel ${tunnel.name}")
        try {
            tunThreads[tunnel.id]?.let {
                if (it.state != Thread.State.TERMINATED) {
                    it.interrupt()
                } else {
                    Timber.d("Thread already terminated")
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to stop tunnel thread for ${tunnel.name}")
        } finally {
            updateTunnelStatus(tunnel, TunnelStatus.Down)
        }
    }

    private fun cleanUpTunThread(tunnel: TunnelConf) {
        Timber.d("Removing thread for ${tunnel.name}")
        tunThreads -= tunnel.id
    }

    private fun removeActiveTunnel(tunnelConf: TunnelConf) {
        activeTuns.update { current -> current.toMutableMap().apply { remove(tunnelConf) } }
    }

    override suspend fun bounceTunnel(tunnelConf: TunnelConf, reason: TunnelStatus.StopReason) {
        bounceTunnelMutex.withLock {
            Timber.i(
                "Bounce tunnel ${tunnelConf.name} for reason: $reason, current bouncing: ${bouncingTunnelIds.size}"
            )
            bouncingTunnelIds[tunnelConf.id] = reason
            try {
                stopTunnel(tunnelConf, reason)
                delay(BOUNCE_DELAY)
                startTunnel(tunnelConf)
            } finally {
                bouncingTunnelIds.remove(tunnelConf.id)
                handleServiceStateOnChange()
                Timber.d(
                    "Cleared bounce state for ${tunnelConf.name}, remaining: ${bouncingTunnelIds.size}"
                )
            }
        }
    }

    override suspend fun runningTunnelNames(): Set<String> =
        activeTuns.value.keys.map { it.tunName }.toSet()

    companion object {
        const val BOUNCE_DELAY = 300L
    }
}
