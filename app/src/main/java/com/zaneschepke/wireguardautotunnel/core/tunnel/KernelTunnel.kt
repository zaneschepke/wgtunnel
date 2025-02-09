package com.zaneschepke.wireguardautotunnel.core.tunnel

import com.wireguard.android.backend.Backend
import com.wireguard.android.backend.Tunnel
import com.zaneschepke.wireguardautotunnel.core.network.NetworkMonitor
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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

class KernelTunnel @Inject constructor(
	@IoDispatcher private val ioDispatcher: CoroutineDispatcher,
	@ApplicationScope private val applicationScope: CoroutineScope,
	private val serviceManager: ServiceManager,
	private val appDataRepository: AppDataRepository,
	private val backend: Backend,
	networkMonitor: NetworkMonitor,
) : TunnelProvider, BaseTunnel(ioDispatcher, applicationScope, networkMonitor, appDataRepository) {

	override suspend fun activeTunnels(): StateFlow<List<TunnelConf>> {
		return super.activeTunnels
	}

	override suspend fun startTunnel(tunnelConf: TunnelConf) {
		withContext(ioDispatcher) {
			if (tunnels.value.any { it.id == tunnelConf.id }) return@withContext Timber.w("Tunnel already running")
			serviceManager.startBackgroundService(tunnelConf)
			appDataRepository.tunnels.save(tunnelConf.copy(isActive = true))
			runCatching {
				backend.setState(tunnelConf, Tunnel.State.UP, tunnelConf.toWgConfig())
			}.onFailure {
				onTunnelStop(tunnelConf)
				Timber.e(it)
			}
		}
	}

	override suspend fun getStatistics(tunnelConf: TunnelConf): TunnelStatistics {
		return WireGuardStatistics(backend.getStatistics(tunnelConf))
	}

	override suspend fun stopTunnel(tunnelConf: TunnelConf?) {
		withContext(ioDispatcher) {
			runCatching {
				tunnelConf?.let {
					backend.setState(it, Tunnel.State.DOWN, tunnelConf.toWgConfig())
					onTunnelStop(tunnelConf)
				} ?: stopAllTunnels()
			}.onFailure {
				Timber.e(it)
			}
		}
	}

	private suspend fun toggleTunnel(tunnelConf: TunnelConf, state: Tunnel.State) {
		withContext(ioDispatcher) {
			backend.setState(tunnelConf, state, tunnelConf.toWgConfig())
		}
	}

	override suspend fun bounceTunnel(tunnelConf: TunnelConf) {
		if (tunnels.value.any { it.id == tunnelConf.id }) {
			toggleTunnel(tunnelConf, Tunnel.State.DOWN)
			toggleTunnel(tunnelConf, Tunnel.State.UP)
		}
	}

	private suspend fun onTunnelStop(tunnelConf: TunnelConf) {
		appDataRepository.tunnels.save(tunnelConf.copy(isActive = false))
		tunnels.update {
			it.toMutableList().apply {
				remove(tunnelConf)
			}
		}
		if (tunnels.value.isEmpty()) serviceManager.stopBackgroundService()
	}

	override suspend fun setBackendState(backendState: BackendState, allowedIps: Collection<String>) {
		Timber.w("Not yet implemented for kernel")
	}

	override suspend fun runningTunnelNames(): Set<String> {
		return backend.runningTunnelNames
	}
}
