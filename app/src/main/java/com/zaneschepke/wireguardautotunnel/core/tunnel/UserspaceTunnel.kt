package com.zaneschepke.wireguardautotunnel.core.tunnel

import com.wireguard.android.backend.BackendException
import com.zaneschepke.wireguardautotunnel.core.network.NetworkMonitor
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
import com.zaneschepke.wireguardautotunnel.util.extensions.toBackendError
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withContext
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
) : TunnelProvider, BaseTunnel(ioDispatcher, applicationScope, networkMonitor, appDataRepository, serviceManager, notificationManager) {

	override suspend fun startTunnel(tunnelConf: TunnelConf) {
		withContext(ioDispatcher) {
			if (tunnels.value.any { it.id == tunnelConf.id }) return@withContext Timber.w("Tunnel already running")
			if (tunnels.value.isNotEmpty()) {
				stopAllTunnels()
			}
			runCatching {
				backend.setState(tunnelConf, Tunnel.State.UP, tunnelConf.toAmConfig())
				super.startTunnel(tunnelConf)
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
		return AmneziaStatistics(backend.getStatistics(tunnelConf))
	}

	override suspend fun toggleTunnel(tunnelConf: TunnelConf, status: TunnelStatus) {
		when (status) {
			TunnelStatus.UP -> backend.setState(tunnelConf, Tunnel.State.UP, tunnelConf.toAmConfig())
			TunnelStatus.DOWN -> backend.setState(tunnelConf, Tunnel.State.DOWN, tunnelConf.toAmConfig())
		}
	}

	override suspend fun stopTunnel(tunnelConf: TunnelConf?) {
		withContext(ioDispatcher) {
			runCatching {
				tunnels.value.firstOrNull { it.id == tunnelConf?.id }?.let {
					backend.setState(it, Tunnel.State.DOWN, it.toAmConfig())
					onTunnelStop(it)
				} ?: stopAllTunnels()
			}.onFailure {
				Timber.e(it)
			}
		}
	}

	override suspend fun setBackendState(backendState: BackendState, allowedIps: Collection<String>) {
		backend.setBackendState(backendState.asAmBackendState(), allowedIps)
	}

	override suspend fun runningTunnelNames(): Set<String> {
		return backend.runningTunnelNames
	}
}
