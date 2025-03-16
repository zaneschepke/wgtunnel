package com.zaneschepke.wireguardautotunnel.core.tunnel

import com.wireguard.android.backend.Backend
import com.wireguard.android.backend.Tunnel
import com.zaneschepke.networkmonitor.NetworkMonitor
import com.zaneschepke.wireguardautotunnel.core.notification.NotificationManager
import com.zaneschepke.wireguardautotunnel.core.service.ServiceManager
import com.zaneschepke.wireguardautotunnel.di.ApplicationScope
import com.zaneschepke.wireguardautotunnel.di.IoDispatcher
import com.zaneschepke.wireguardautotunnel.domain.entity.TunnelConf
import com.zaneschepke.wireguardautotunnel.domain.enums.BackendState
import com.zaneschepke.wireguardautotunnel.domain.enums.TunnelStatus
import com.zaneschepke.wireguardautotunnel.domain.repository.AppDataRepository
import com.zaneschepke.wireguardautotunnel.domain.state.TunnelStatistics
import com.zaneschepke.wireguardautotunnel.domain.state.WireGuardStatistics
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

class KernelTunnel @Inject constructor(
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
		Timber.i("Starting tunnel ${tunnelConf.id} kernel")
		applicationScope.launch(ioDispatcher) {
			runCatching {
				updateTunnelState(tunnelConf.id, TunnelStatus.STARTING)
				Timber.d("Set STARTING state for tunnel ${tunnelConf.id} at ${System.currentTimeMillis()}")

				runBlocking { configureTunnel(tunnelConf) }
				Timber.d("Callback set for tunnel ${tunnelConf.id} at ${System.currentTimeMillis()}")

				super.startTunnel(tunnelConf)
				Timber.d("Calling backend.setState UP for tunnel ${tunnelConf.id}")
				backend.setState(tunnelConf, Tunnel.State.UP, tunnelConf.toWgConfig())
				startedTunnels[tunnelConf.id] = tunnelConf

				val backendState = backend.getState(tunnelConf)
				if (backendState == Tunnel.State.UP) {
					updateTunnelState(tunnelConf.id, TunnelStatus.UP)
					Timber.d("Confirmed UP state for tunnel ${tunnelConf.id} at ${System.currentTimeMillis()}")
				} else {
					Timber.w("Tunnel ${tunnelConf.id} not UP after setState, state: $backendState")
				}

				// Start stats jobs only after UP is confirmed
				tunnelJobs[tunnelConf.id] = mutableListOf(startTunnelJobs(tunnelConf))
				Timber.d("Started stats jobs for tunnel ${tunnelConf.id} at ${System.currentTimeMillis()}")
			}.onFailure { exception ->
				Timber.e(exception, "Failed to start tunnel ${tunnelConf.id} kernel")
				stopTunnel(tunnelConf)
				handleBackendThrowable(exception)
			}.onSuccess {
				Timber.i("Tunnel ${tunnelConf.id} started successfully")
			}
		}
	}

	override fun getStatistics(tunnelConf: TunnelConf): TunnelStatistics? {
		return try {
			WireGuardStatistics(backend.getStatistics(tunnelConf))
		} catch (e: Exception) {
			Timber.e(e)
			null
		}
	}

	override fun stopTunnel(tunnelConf: TunnelConf?) {
		applicationScope.launch(ioDispatcher) {
			runCatching {
				val originalTunnel = tunnelConf?.let { startedTunnels.getOrDefault(it.id, null) }
				if (originalTunnel != null) {
					Timber.i(
						"Stopping tunnel ${originalTunnel.id} kernel",
					)
//					updateTunnelState(tunnelConf.id, TunnelStatus.STOPPING)
					backend.setState(originalTunnel, Tunnel.State.DOWN, originalTunnel.toWgConfig())
					super.stopTunnel(originalTunnel)
					startedTunnels.remove(originalTunnel.id)
					tunnelJobs[originalTunnel.id]?.forEach { it.cancel() }
					tunnelJobs.remove(originalTunnel.id)
					if (backend.getState(originalTunnel) == Tunnel.State.DOWN) {
						updateTunnelState(originalTunnel.id, TunnelStatus.DOWN)
						Timber.d("Confirmed DOWN state for tunnel ${originalTunnel.id}")
					}
				} else {
					Timber.w("Tunnel not found in startedTunnels, stopping all tunnels")
					startedTunnels.forEach { (_, config) ->
//						updateTunnelState(config.id, TunnelStatus.STOPPING)
						val state = backend.setState(config, Tunnel.State.DOWN, config.toWgConfig())
						super.stopTunnel(tunnelConf)
						if (state == Tunnel.State.DOWN) {
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
		Timber.w("Not yet implemented for kernel")
	}

	override suspend fun runningTunnelNames(): Set<String> {
		return backend.runningTunnelNames
	}
}
