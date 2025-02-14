package com.zaneschepke.wireguardautotunnel.core.tunnel

import com.wireguard.android.backend.Backend
import com.wireguard.android.backend.BackendException
import com.wireguard.android.backend.Tunnel
import com.zaneschepke.wireguardautotunnel.core.network.NetworkMonitor
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
import com.zaneschepke.wireguardautotunnel.util.extensions.toBackendError
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withContext
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

	override suspend fun startTunnel(tunnelConf: TunnelConf) {
		withContext(ioDispatcher) {
			super.startTunnel(tunnelConf)
			runCatching {
				backend.setState(tunnelConf, Tunnel.State.UP, tunnelConf.toWgConfig())
				addToActiveTunnels(tunnelConf)
			}.onFailure {
				onTunnelStop(tunnelConf)
				if (it is BackendException) {
					handleBackendThrowable(it.toBackendError())
				} else {
					Timber.e(it)
				}
			}
		}
	}

	override suspend fun getStatistics(tunnelConf: TunnelConf): TunnelStatistics {
		return WireGuardStatistics(backend.getStatistics(tunnelConf))
	}

	override suspend fun stopTunnel(tunnelConf: TunnelConf?) {
		withContext(ioDispatcher) {
			val tunnel = tunnels.value.firstOrNull { it.id == tunnelConf?.id }
			runCatching {
				tunnel?.let {
					backend.setState(it, Tunnel.State.DOWN, it.toWgConfig())
					onTunnelStop(it)
				} ?: stopAllTunnels()
			}.onFailure {
				Timber.e(it)
			}
		}
	}

	override suspend fun toggleTunnel(tunnelConf: TunnelConf, status: TunnelStatus) {
		when (status) {
			TunnelStatus.UP -> backend.setState(tunnelConf, Tunnel.State.UP, tunnelConf.toWgConfig())
			TunnelStatus.DOWN -> backend.setState(tunnelConf, Tunnel.State.DOWN, tunnelConf.toWgConfig())
		}
	}

	override suspend fun setBackendState(backendState: BackendState, allowedIps: Collection<String>) {
		Timber.w("Not yet implemented for kernel")
	}

	override suspend fun runningTunnelNames(): Set<String> {
		return backend.runningTunnelNames
	}
}
