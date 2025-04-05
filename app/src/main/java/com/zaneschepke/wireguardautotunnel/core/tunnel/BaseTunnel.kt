package com.zaneschepke.wireguardautotunnel.core.tunnel

import com.wireguard.android.backend.Tunnel
import com.zaneschepke.wireguardautotunnel.core.service.ServiceManager
import com.zaneschepke.wireguardautotunnel.di.ApplicationScope
import com.zaneschepke.wireguardautotunnel.di.IoDispatcher
import com.zaneschepke.wireguardautotunnel.domain.entity.TunnelConf
import com.zaneschepke.wireguardautotunnel.domain.enums.BackendError
import com.zaneschepke.wireguardautotunnel.domain.enums.TunnelStatus
import com.zaneschepke.wireguardautotunnel.domain.repository.AppDataRepository
import com.zaneschepke.wireguardautotunnel.domain.state.TunnelState
import com.zaneschepke.wireguardautotunnel.domain.state.TunnelStatistics
import com.zaneschepke.wireguardautotunnel.util.extensions.asTunnelState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

abstract class BaseTunnel(
	@IoDispatcher private val ioDispatcher: CoroutineDispatcher,
	@ApplicationScope private val applicationScope: CoroutineScope,
	private val appDataRepository: AppDataRepository,
	private val serviceManager: ServiceManager,
) : TunnelProvider {

	private val activeTuns = MutableStateFlow<Map<TunnelConf, TunnelState>>(emptyMap())
	private val tunThreads = ConcurrentHashMap<Int, Thread>()
	override val activeTunnels = activeTuns.asStateFlow()

	private val isBounce = AtomicBoolean(false)

	private val tunMutex = Mutex()
	private val tunStatusMutex = Mutex()

	abstract suspend fun startBackend(tunnel: TunnelConf)

	abstract fun stopBackend(tunnel: TunnelConf)

	override fun clearError(tunnelConf: TunnelConf) {
		applicationScope.launch {
			updateTunnelStatus(tunnelConf, TunnelStatus.Down)
		}
	}

	override fun hasVpnPermission(): Boolean {
		return serviceManager.hasVpnPermission()
	}

	protected suspend fun updateTunnelStatus(tunnelConf: TunnelConf, state: TunnelStatus? = null, stats: TunnelStatistics? = null) {
		tunStatusMutex.withLock {
			activeTuns.update { current ->
				val originalConf = current.getKeyById(tunnelConf.id) ?: tunnelConf
				val existingState = current.getValueById(tunnelConf.id) ?: TunnelState()
				val newState = state ?: existingState.status
				if (newState == TunnelStatus.Down) {
					Timber.d("Removing tunnel ${tunnelConf.id} from activeTunnels as state is DOWN")
					current - originalConf
				} else if (existingState.status == newState && stats == null) {
					Timber.d("Skipping redundant state update for ${tunnelConf.id}: $newState")
					current
				} else {
					Timber.d("Updating tunnel ${tunnelConf.id} in activeTunnels as state is $newState, with stats")
					val updated = existingState.copy(
						status = newState,
						statistics = stats ?: existingState.statistics,
					)
					current + (originalConf to updated)
				}
			}
		}
	}

	private suspend fun stopActiveTunnels() {
		activeTunnels.value.forEach { (config, state) ->
			if (state.status.isUpOrStarting()) {
				stopTunnel(config)
				delay(300)
			}
		}
	}

	private fun configureTunnelCallbacks(tunnelConf: TunnelConf) {
		Timber.d("Configuring TunnelConf instance: ${tunnelConf.hashCode()}")

		tunnelConf.setStateChangeCallback { state ->
			Timber.d("State change callback triggered for tunnel ${tunnelConf.id}: ${tunnelConf.tunName} with state $state at ${System.currentTimeMillis()}")
			when (state) {
				is Tunnel.State -> applicationScope.launch { updateTunnelStatus(tunnelConf, state.asTunnelState()) }
				is org.amnezia.awg.backend.Tunnel.State -> applicationScope.launch { updateTunnelStatus(tunnelConf, state.asTunnelState()) }
			}
			serviceManager.updateTunnelTile()
		}
		tunnelConf.setTunnelStatsCallback {
			val stats = getStatistics(tunnelConf)
			applicationScope.launch {
				updateTunnelStatus(tunnelConf, null, stats)
			}
		}
		tunnelConf.setBounceTunnelCallback(::bounceTunnel)
	}

	override fun startTunnel(tunnelConf: TunnelConf) {
		if (activeTuns.exists(tunnelConf.id) || tunThreads.containsKey(tunnelConf.id)) return
		// use thread to interrupt java backend if stuck (like in dns resolution)
		tunThreads += tunnelConf.id to thread {
			runBlocking {
				// stop active tunnels if we are userspace
				if (this@BaseTunnel is UserspaceTunnel) stopActiveTunnels()
				try {
					Timber.d("Starting tunnel ${tunnelConf.id}...")
					startTunnelInner(tunnelConf)
					Timber.d("Started complete for tunnel ${tunnelConf.name}...")
				} catch (e: BackendError) {
					Timber.e(e, "Failed to start tunnel ${tunnelConf.name} userspace")
					updateTunnelStatus(tunnelConf, TunnelStatus.Error(e))
				} catch (e: InterruptedException) {
					Timber.i("Tunnel start has been interrupted as ${tunnelConf.name} failed to start")
				}
			}
		}
	}

	private suspend fun startTunnelInner(tunnelConf: TunnelConf) {
		tunMutex.withLock {
			configureTunnelCallbacks(tunnelConf)
			Timber.d("Started backend for tunnel ${tunnelConf.id}...")
			startBackend(tunnelConf)
			updateTunnelStatus(tunnelConf, TunnelStatus.Up)
			Timber.d("DONE for tun ${tunnelConf.id}...")
			saveTunnelActiveState(tunnelConf, true)
			if (!isBounce.get()) serviceManager.startTunnelForegroundService(tunnelConf)
		}
	}

	private suspend fun saveTunnelActiveState(tunnelConf: TunnelConf, active: Boolean) {
		val tunnelCopy = tunnelConf.copyWithCallback(isActive = active)
		appDataRepository.tunnels.save(tunnelCopy)
	}

	override fun stopTunnel(tunnelConf: TunnelConf?, reason: TunnelStatus.StopReason) {
		applicationScope.launch(ioDispatcher) {
			if (tunnelConf == null) return@launch stopActiveTunnels()
			try {
				val stuckStarting = activeTuns.isStarting(tunnelConf.id)
				handleTunnelThreadCleanup(tunnelConf)
				if (stuckStarting) return@launch
				updateTunnelStatus(tunnelConf, TunnelStatus.Stopping(reason))
				stopTunnelInner(tunnelConf)
			} catch (e: BackendError) {
				Timber.e(e, "Failed to stop tunnel ${tunnelConf.id}")
				updateTunnelStatus(tunnelConf, TunnelStatus.Error(e))
			}
		}
	}

	private suspend fun stopTunnelInner(tunnelConf: TunnelConf) {
		tunMutex.withLock {
			val tunnel = activeTuns.findTunnel(tunnelConf.id) ?: return
			stopBackend(tunnel)
			saveTunnelActiveState(tunnelConf, false)
			removeActiveTunnel(tunnel)
			handleServiceChangesOnStop()
		}
	}

	private fun handleServiceChangesOnStop() {
		if (activeTuns.value.isEmpty() && !isBounce.get()) return serviceManager.stopTunnelForegroundService()
		val nextActive = activeTuns.value.keys.firstOrNull()
		if (nextActive != null) {
			Timber.d("Next active tunnel: ${nextActive.id}")
			serviceManager.updateTunnelForegroundServiceNotification(nextActive)
		}
	}

	private suspend fun handleTunnelThreadCleanup(tunnel: TunnelConf) {
		Timber.d("Cleaning up thread for ${tunnel.name}")
		try {
			tunThreads[tunnel.id]?.let {
				if (it.state != Thread.State.TERMINATED) {
					it.interrupt()
					updateTunnelStatus(tunnel, TunnelStatus.Down)
				} else {
					Timber.d("Thread already terminated")
				}
			}
		} catch (e: Exception) {
			Timber.e(e, "Failed to stop tunnel thread for ${tunnel.name}")
		}
		Timber.d("Removing thread for ${tunnel.name}")
		tunThreads -= tunnel.id
	}

	private fun removeActiveTunnel(tunnelConf: TunnelConf) {
		activeTuns.update { current ->
			current.toMutableMap().apply { remove(tunnelConf) }
		}
	}

	override fun bounceTunnel(tunnelConf: TunnelConf, reason: TunnelStatus.StopReason) {
		applicationScope.launch(ioDispatcher) {
			Timber.i("Bounce tunnel ${tunnelConf.name}")
			isBounce.set(true)
			stopTunnel(tunnelConf, reason)
			delay(300)
			startTunnel(tunnelConf)
			isBounce.set(false)
		}
	}

	override suspend fun runningTunnelNames(): Set<String> = activeTuns.value.keys.map { it.tunName }.toSet()
}
