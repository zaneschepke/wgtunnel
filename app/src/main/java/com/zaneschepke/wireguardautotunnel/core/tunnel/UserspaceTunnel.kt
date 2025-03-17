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
import com.zaneschepke.wireguardautotunnel.util.extensions.asTunnelState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import org.amnezia.awg.backend.Backend
import org.amnezia.awg.backend.Tunnel
import timber.log.Timber
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

	override fun startTunnel(tunnelConf: TunnelConf) {
		Timber.i("Starting tunnel ${tunnelConf.id} userspace")
		applicationScope.launch(ioDispatcher) {
			runCatching {
				// tunnel already active
				if (activeTuns.value.any { it.key.id == tunnelConf.id }) return@launch

				// stop any active tunnels that aren't this one, userspace only
				stopActiveTunnels()

				mutex.withLock {
					updateTunnelState(tunnelConf, TunnelStatus.STARTING)

					// configure state callback and add to tunnels
					configureTunnel(tunnelConf)

					updateTunnelState(tunnelConf, backend.setState(tunnelConf, Tunnel.State.UP, tunnelConf.toAmConfig()).asTunnelState())

					// run some actions after start success
					onStartSuccess(tunnelConf)
				}
			}.onFailure { exception ->
				Timber.e(exception, "Failed to start tunnel ${tunnelConf.id} userspace")
				stopTunnel(tunnelConf)
				handleBackendThrowable(exception)
			}.onSuccess {
				Timber.i("Tunnel ${tunnelConf.id} started successfully")
			}
		}
	}

	private suspend fun stopActiveTunnels() {
		activeTunnels.value.forEach { (config, state) ->
			if (state.state.isUp()) {
				stopTunnel(config)
				delay(300)
			}
		}
	}

	override fun stopTunnel(tunnelConf: TunnelConf?) {
		applicationScope.launch(ioDispatcher) {
			runCatching {
				val originalTunnel = activeTuns.value.keys.find { it.id == tunnelConf?.id }
				if (originalTunnel != null) {
					Timber.i("Stopping tunnel ${originalTunnel.id} userspace")
					mutex.withLock {
						updateTunnelState(originalTunnel, backend.setState(originalTunnel, Tunnel.State.DOWN, originalTunnel.toAmConfig()).asTunnelState())
						super.stopTunnel(originalTunnel)
					}
				} else {
					Timber.w("Tunnel not found in startedTunnels, stopping all tunnels")
					activeTuns.value.keys.forEach { config ->
						stopTunnel(config)
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
