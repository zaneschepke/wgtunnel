package com.zaneschepke.wireguardautotunnel.core.tunnel

import com.wireguard.android.backend.Backend
import com.wireguard.android.backend.Tunnel
import com.zaneschepke.wireguardautotunnel.core.notification.NotificationManager
import com.zaneschepke.wireguardautotunnel.core.service.ServiceManager
import com.zaneschepke.wireguardautotunnel.di.ApplicationScope
import com.zaneschepke.wireguardautotunnel.di.IoDispatcher
import com.zaneschepke.wireguardautotunnel.domain.entity.TunnelConf
import com.zaneschepke.wireguardautotunnel.domain.enums.BackendState
import com.zaneschepke.wireguardautotunnel.domain.repository.AppDataRepository
import com.zaneschepke.wireguardautotunnel.domain.state.TunnelStatistics
import com.zaneschepke.wireguardautotunnel.domain.state.WireGuardStatistics
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import javax.inject.Inject

class KernelTunnel @Inject constructor(
	@IoDispatcher private val ioDispatcher: CoroutineDispatcher,
	@ApplicationScope private val applicationScope: CoroutineScope,
	serviceManager: ServiceManager,
	appDataRepository: AppDataRepository,
	notificationManager: NotificationManager,
	private val backend: Backend,
) : BaseTunnel(ioDispatcher, applicationScope, appDataRepository, serviceManager, notificationManager) {

	override fun startTunnel(tunnelConf: TunnelConf) {
		Timber.i("Starting tunnel ${tunnelConf.id} kernel")
		applicationScope.launch(ioDispatcher) {
			runCatching {
				// tunnel already active
				if (activeTuns.value.any { it.key.id == tunnelConf.id }) return@launch

				mutex.withLock {
					// configure state callback and add to tunnels
					configureTunnel(tunnelConf)
					backend.setState(tunnelConf, Tunnel.State.UP, tunnelConf.toWgConfig())
					super.startTunnel(tunnelConf)
				}
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
				mutex.withLock {
					val originalTunnel = activeTuns.value.keys.find { it.id == tunnelConf?.id }
					if (originalTunnel == null) return@launch stopActiveTunnels()
					Timber.i("Stopping tunnel ${originalTunnel.id} kernel")
					backend.setState(originalTunnel, Tunnel.State.DOWN, originalTunnel.toWgConfig())
					super.stopTunnel(originalTunnel)
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
