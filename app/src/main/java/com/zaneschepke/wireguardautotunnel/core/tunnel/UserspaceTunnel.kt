package com.zaneschepke.wireguardautotunnel.core.tunnel

import com.wireguard.android.backend.BackendException
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
import com.zaneschepke.wireguardautotunnel.util.extensions.toBackendError
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
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
		applicationScope.launch(ioDispatcher) {
			Timber.d("Starting tunnel ${tunnelConf.id} userspace")
			if (tunnels.value.any { it.id == tunnelConf.id }) return@launch Timber.w("Tunnel already running")
			if (tunnels.value.isNotEmpty()) {
				Timber.d("Stopping all tunnels")
				stopAllTunnels()
			}
			runCatching {
				Timber.d("Setting backend state UP")
				backend.setState(tunnelConf, Tunnel.State.UP, tunnelConf.toAmConfig())
				Timber.d("Calling super.startTunnel")
				super.startTunnel(tunnelConf)
			}.onFailure {
				Timber.e(it, "Failed to start tunnel ${tunnelConf.id} userspace")
				onTunnelStop(tunnelConf)
				if (it is BackendException) {
					handleBackendThrowable(it.toBackendError())
				} else {
					Timber.e(it)
				}
			}
		}
	}

	override fun toggleTunnel(tunnelConf: TunnelConf, status: TunnelStatus) {
		applicationScope.launch(ioDispatcher) {
			runCatching {
				when (status) {
					TunnelStatus.UP -> backend.setState(tunnelConf, Tunnel.State.UP, tunnelConf.toAmConfig())
					TunnelStatus.DOWN -> backend.setState(tunnelConf, Tunnel.State.DOWN, tunnelConf.toAmConfig())
				}
			}.onFailure {
				Timber.e(it)
			}
		}
	}

	override fun stopTunnel(tunnelConf: TunnelConf?) {
		applicationScope.launch(ioDispatcher) {
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

	override suspend fun bounceTunnel(tunnelConf: TunnelConf) {
		if (tunnels.value.any { it.id == tunnelConf.id }) {
			toggleTunnel(tunnelConf, TunnelStatus.DOWN)
			toggleTunnel(tunnelConf, TunnelStatus.UP)
		}
	}

	override suspend fun setBackendState(backendState: BackendState, allowedIps: Collection<String>) {
		backend.setBackendState(backendState.asAmBackendState(), allowedIps)
	}

	override suspend fun runningTunnelNames(): Set<String> {
		return backend.runningTunnelNames
	}

	override fun getStatistics(tunnelConf: TunnelConf): TunnelStatistics {
		return AmneziaStatistics(backend.getStatistics(tunnelConf))
	}
}
