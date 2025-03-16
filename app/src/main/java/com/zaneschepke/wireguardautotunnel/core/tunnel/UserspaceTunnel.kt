package com.zaneschepke.wireguardautotunnel.core.tunnel

import com.zaneschepke.networkmonitor.NetworkMonitor
import com.zaneschepke.wireguardautotunnel.core.notification.NotificationManager
import com.zaneschepke.wireguardautotunnel.core.service.ServiceManager
import com.zaneschepke.wireguardautotunnel.di.ApplicationScope
import com.zaneschepke.wireguardautotunnel.di.IoDispatcher
import com.zaneschepke.wireguardautotunnel.domain.entity.TunnelConf
import com.zaneschepke.wireguardautotunnel.domain.enums.BackendState
import com.zaneschepke.wireguardautotunnel.domain.enums.TunnelStatus
import com.zaneschepke.wireguardautotunnel.domain.repository.AppDataRepository
import com.zaneschepke.wireguardautotunnel.domain.state.AmneziaStatistics
import com.zaneschepke.wireguardautotunnel.domain.state.TunnelStatistics
import com.zaneschepke.wireguardautotunnel.util.extensions.asAmBackendState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.amnezia.awg.backend.Backend
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

class UserspaceTunnel @Inject constructor(
	@IoDispatcher private val ioDispatcher: CoroutineDispatcher,
	@ApplicationScope private val applicationScope: CoroutineScope,
	serviceManager: ServiceManager,
	appDataRepository: AppDataRepository,
	notificationManager: NotificationManager,
	private val backend: Backend,
	networkMonitor: NetworkMonitor,
) : BaseTunnel(ioDispatcher, applicationScope, networkMonitor, appDataRepository, serviceManager, notificationManager) {

	private val startedTunnels = ConcurrentHashMap<Int, TunnelConf>()

	override fun startTunnel(tunnelConf: TunnelConf) {
		Timber.i("Starting tunnel ${tunnelConf.id} userspace")
		applicationScope.launch(ioDispatcher) {
			runCatching {
				stopActiveTunnels(tunnelConf)
				updateTunnelState(tunnelConf.id, TunnelStatus.STARTING)
				Timber.d("Set STARTING state for tunnel ${tunnelConf.id} at ${System.currentTimeMillis()}")

				runBlocking { configureTunnel(tunnelConf) }
				Timber.d("Callback set for tunnel ${tunnelConf.id} at ${System.currentTimeMillis()}")

				super.startTunnel(tunnelConf)
				Timber.d("Calling backend.setState UP for tunnel ${tunnelConf.id} with (identity: ${System.identityHashCode(tunnelConf)})")
				backend.setState(tunnelConf, org.amnezia.awg.backend.Tunnel.State.UP, tunnelConf.toAmConfig())
				startedTunnels[tunnelConf.id] = tunnelConf

				val backendState = backend.getState(tunnelConf)
				if (backendState == org.amnezia.awg.backend.Tunnel.State.UP) {
					updateTunnelState(tunnelConf.id, TunnelStatus.UP)
					Timber.d("Confirmed UP state for tunnel ${tunnelConf.id} at ${System.currentTimeMillis()}")
				} else {
					Timber.w("Tunnel ${tunnelConf.id} not UP after setState, state: $backendState")
				}

				// Start stats jobs only after UP is confirmed
				tunnelJobs[tunnelConf.id] = mutableListOf(startTunnelJobs(tunnelConf))
				Timber.d("Started stats jobs for tunnel ${tunnelConf.id} at ${System.currentTimeMillis()}")
			}.onFailure { exception ->
				Timber.e(exception, "Failed to start tunnel ${tunnelConf.id} userspace")
				stopTunnel(tunnelConf)
				handleBackendThrowable(exception)
			}.onSuccess {
				Timber.i("Tunnel ${tunnelConf.id} started successfully")
			}
		}
	}

	private suspend fun stopActiveTunnels(tunnelConf: TunnelConf) {
		val runningTunnels = activeTunnels.value.filter { (id, state) ->
			id != tunnelConf.id && state.state == TunnelStatus.UP
		}
		runningTunnels.forEach { (id, _) ->
			val runningTunnel = startedTunnels[id]
			if (runningTunnel != null) {
				Timber.i("Stopping running tunnel ${runningTunnel.id} before starting ${tunnelConf.id}")
				stopTunnel(runningTunnel)
				delay(300)
			}
		}
	}

	override fun stopTunnel(tunnelConf: TunnelConf?) {
		applicationScope.launch(ioDispatcher) {
			runCatching {
				val originalTunnel = tunnelConf?.let { startedTunnels.getOrDefault(it.id, null) }
				if (originalTunnel != null) {
					Timber.i("Stopping tunnel ${originalTunnel.id} userspace with original TunnelConf: $originalTunnel (identity: ${System.identityHashCode(originalTunnel)})")
					backend.setState(originalTunnel, org.amnezia.awg.backend.Tunnel.State.DOWN, originalTunnel.toAmConfig())
					super.stopTunnel(originalTunnel)
					startedTunnels.remove(originalTunnel.id)
					tunnelJobs[originalTunnel.id]?.forEach { it.cancel() }
					tunnelJobs.remove(originalTunnel.id)
					if (backend.getState(originalTunnel) == org.amnezia.awg.backend.Tunnel.State.DOWN) {
						updateTunnelState(originalTunnel.id, TunnelStatus.DOWN)
						Timber.d("Confirmed DOWN state for tunnel ${originalTunnel.id}")
					}
				} else {
					Timber.w("Tunnel not found in startedTunnels, stopping all tunnels")
					startedTunnels.forEach { (_, config) ->
						val state = backend.setState(config, org.amnezia.awg.backend.Tunnel.State.DOWN, config.toAmConfig())
						super.stopTunnel(tunnelConf)
						if (state == org.amnezia.awg.backend.Tunnel.State.DOWN) {
							startedTunnels.remove(config.id)
							tunnelJobs[config.id]?.forEach { it.cancel() }
							tunnelJobs.remove(config.id)
							updateTunnelState(config.id, TunnelStatus.DOWN)
							Timber.d("Confirmed DOWN state for tunnel ${config.id} after fallback")
						}
					}
				}
			}.onFailure { e ->
				Timber.e(e, "Failed to stop tunnel ${tunnelConf?.id}")
			}
		}
	}

	override suspend fun setBackendState(backendState: BackendState, allowedIps: Collection<String>) {
		Timber.d("Setting backend state: $backendState with allowedIps: $allowedIps")
		backend.setBackendState(backendState.asAmBackendState(), allowedIps)
	}

	override suspend fun runningTunnelNames(): Set<String> {
		return backend.runningTunnelNames
	}

	override fun getStatistics(tunnelConf: TunnelConf): TunnelStatistics? {
		return try {
			AmneziaStatistics(backend.getStatistics(tunnelConf))
		} catch (e: Exception) {
			Timber.e(e, "Failed to get stats for ${tunnelConf.tunName}")
			null
		}
	}
}
