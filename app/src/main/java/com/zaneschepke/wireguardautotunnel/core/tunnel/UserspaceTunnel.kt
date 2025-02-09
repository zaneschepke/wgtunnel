package com.zaneschepke.wireguardautotunnel.core.tunnel

import com.zaneschepke.wireguardautotunnel.core.network.NetworkMonitor
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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import org.amnezia.awg.backend.Backend
import org.amnezia.awg.backend.Tunnel
import timber.log.Timber
import javax.inject.Inject

class UserspaceTunnel @Inject constructor(
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
			if (tunnels.value.isNotEmpty()) stopAllTunnels()
			serviceManager.startBackgroundService(tunnelConf)
			appDataRepository.tunnels.save(tunnelConf.copy(isActive = true))
			runCatching {
				backend.setState(tunnelConf, Tunnel.State.UP, tunnelConf.toAmConfig())
				addActiveTunnel(tunnelConf)
			}.onFailure {
				onTunnelStop(tunnelConf)
				Timber.e(it)
			}
		}
	}

	private fun addActiveTunnel(tunnelConf: TunnelConf) {
		tunnels.update {
			it.toMutableList().apply {
				add(tunnelConf)
			}
		}
	}

	override suspend fun getStatistics(tunnelConf: TunnelConf): TunnelStatistics {
		return AmneziaStatistics(backend.getStatistics(tunnelConf))
	}

	override suspend fun stopTunnel(tunnelConf: TunnelConf?) {
		withContext(ioDispatcher) {
			runCatching {
				activeTunnels.value.firstOrNull { it.id == tunnelConf?.id }?.let {
					backend.setState(it, Tunnel.State.DOWN, it.toAmConfig())
					onTunnelStop(it)
				} ?: stopAllTunnels()
			}.onFailure {
				Timber.e(it)
			}
		}
	}

	private suspend fun toggleTunnel(tunnelConf: TunnelConf, state: Tunnel.State) {
		withContext(ioDispatcher) {
			runCatching {
				backend.setState(tunnelConf, state, tunnelConf.toAmConfig())
			}.onFailure {
				Timber.e(it)
			}
		}
	}

	override suspend fun bounceTunnel(tunnelConf: TunnelConf) {
		if (tunnels.value.any { it.id == tunnelConf.id }) {
			toggleTunnel(tunnelConf, Tunnel.State.DOWN)
			toggleTunnel(tunnelConf, Tunnel.State.UP)
		}
	}

	private fun removeActiveTunnel(tunnelConf: TunnelConf) {
		tunnels.update {
			it.toMutableList().apply {
				remove(tunnelConf)
			}
		}
	}

	private suspend fun onTunnelStop(tunnelConf: TunnelConf) {
		appDataRepository.tunnels.save(tunnelConf.copy(isActive = false))
		removeActiveTunnel(tunnelConf)
		if (tunnels.value.isEmpty()) serviceManager.stopBackgroundService()
	}

	override suspend fun setBackendState(backendState: BackendState, allowedIps: Collection<String>) {
		backend.setBackendState(backendState.asAmBackendState(), allowedIps)
	}

	override suspend fun runningTunnelNames(): Set<String> {
		return backend.runningTunnelNames
	}
}
