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

	override fun getStatistics(tunnelConf: TunnelConf): TunnelStatistics? {
		return try {
			WireGuardStatistics(backend.getStatistics(tunnelConf))
		} catch (e: Exception) {
			Timber.e(e)
			null
		}
	}

	override suspend fun startBackend(tunnel: TunnelConf) {
		backend.setState(tunnel, Tunnel.State.UP, tunnel.toWgConfig())
	}

	override fun stopBackend(tunnel: TunnelConf) {
		Timber.i("Stopping tunnel ${tunnel.id} kernel")
		backend.setState(tunnel, Tunnel.State.DOWN, tunnel.toWgConfig())
	}

	override suspend fun setBackendState(backendState: BackendState, allowedIps: Collection<String>) {
		Timber.w("Not yet implemented for kernel")
	}

	override suspend fun runningTunnelNames(): Set<String> {
		return backend.runningTunnelNames
	}
}
