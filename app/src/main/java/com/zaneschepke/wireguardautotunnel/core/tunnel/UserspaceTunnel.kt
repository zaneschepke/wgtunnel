package com.zaneschepke.wireguardautotunnel.core.tunnel

import com.zaneschepke.wireguardautotunnel.core.notification.NotificationManager
import com.zaneschepke.wireguardautotunnel.core.service.ServiceManager
import com.zaneschepke.wireguardautotunnel.di.ApplicationScope
import com.zaneschepke.wireguardautotunnel.di.IoDispatcher
import com.zaneschepke.wireguardautotunnel.domain.entity.TunnelConf
import com.zaneschepke.wireguardautotunnel.domain.enums.BackendState
import com.zaneschepke.wireguardautotunnel.domain.repository.AppDataRepository
import com.zaneschepke.wireguardautotunnel.domain.state.AmneziaStatistics
import com.zaneschepke.wireguardautotunnel.domain.state.TunnelStatistics
import com.zaneschepke.wireguardautotunnel.util.extensions.asAmBackendState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
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
) : BaseTunnel(ioDispatcher, applicationScope, appDataRepository, serviceManager, notificationManager) {

	override suspend fun startBackend(tunnel: TunnelConf) {
		stopActiveTunnels()
		backend.setState(tunnel, Tunnel.State.UP, tunnel.toAmConfig())
	}

	override fun stopBackend(tunnel: TunnelConf) {
		Timber.i("Stopping tunnel ${tunnel.id} userspace")
		backend.setState(tunnel, Tunnel.State.DOWN, tunnel.toAmConfig())
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
