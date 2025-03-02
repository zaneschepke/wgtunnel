package com.zaneschepke.wireguardautotunnel.core.tunnel

import com.wireguard.android.backend.Backend
import com.wireguard.android.backend.BackendException
import com.wireguard.android.backend.Tunnel
import com.zaneschepke.networkmonitor.NetworkMonitor
import com.zaneschepke.wireguardautotunnel.core.notification.NotificationManager
import com.zaneschepke.wireguardautotunnel.core.service.ServiceManager
import com.zaneschepke.wireguardautotunnel.di.ApplicationScope
import com.zaneschepke.wireguardautotunnel.di.IoDispatcher
import com.zaneschepke.wireguardautotunnel.domain.entity.TunnelConf
import com.zaneschepke.wireguardautotunnel.domain.enums.BackendState
import com.zaneschepke.wireguardautotunnel.domain.repository.AppDataRepository
import com.zaneschepke.wireguardautotunnel.domain.state.TunnelStatistics
import com.zaneschepke.wireguardautotunnel.domain.state.WireGuardStatistics
import com.zaneschepke.wireguardautotunnel.util.extensions.toBackendError
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
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

	override fun startTunnel(tunnelConf: TunnelConf) {
		Timber.d("Starting tunnel ${tunnelConf.id} kernel")
		applicationScope.launch(ioDispatcher) {
			if (tunnels.value.any { it.id == tunnelConf.id }) return@launch Timber.w("Tunnel already running")
			runCatching {
				Timber.d("Setting backend state UP")
				super.beforeStartTunnel(tunnelConf)
				backend.setState(tunnelConf, Tunnel.State.UP, tunnelConf.toWgConfig())
				Timber.d("Calling super.startTunnel")
				super.startTunnel(tunnelConf)
			}.onFailure {
				Timber.e(it, "Failed to start tunnel ${tunnelConf.id} kernel")
				onTunnelStop(tunnelConf)
				if (it is BackendException) {
					handleBackendThrowable(it.toBackendError())
				} else {
					Timber.e(it)
				}
			}
		}
	}

	override fun getStatistics(tunnelConf: TunnelConf): TunnelStatistics {
		return WireGuardStatistics(backend.getStatistics(tunnelConf))
	}

	override fun stopTunnel(tunnelConf: TunnelConf?) {
		applicationScope.launch(ioDispatcher) {
			runCatching {
				tunnels.value.firstOrNull { it.id == tunnelConf?.id }?.let {
					backend.setState(it, Tunnel.State.DOWN, it.toWgConfig())
					onTunnelStop(it)
				} ?: stopAllTunnels()
			}.onFailure {
				Timber.e(it)
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
