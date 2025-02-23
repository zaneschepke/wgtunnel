package com.zaneschepke.wireguardautotunnel.core.tunnel

import com.zaneschepke.wireguardautotunnel.di.ApplicationScope
import com.zaneschepke.wireguardautotunnel.di.IoDispatcher
import com.zaneschepke.wireguardautotunnel.di.Kernel
import com.zaneschepke.wireguardautotunnel.di.Userspace
import com.zaneschepke.wireguardautotunnel.domain.entity.AppSettings
import com.zaneschepke.wireguardautotunnel.domain.entity.TunnelConf
import com.zaneschepke.wireguardautotunnel.domain.enums.BackendState
import com.zaneschepke.wireguardautotunnel.domain.repository.AppDataRepository
import com.zaneschepke.wireguardautotunnel.util.extensions.withData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.plus
import kotlinx.coroutines.withContext
import javax.inject.Inject

class TunnelManager @Inject constructor(
	@Kernel private val kernelTunnel: TunnelProvider,
	@Userspace private val userspaceTunnel: TunnelProvider,
	private val appDataRepository: AppDataRepository,
	@ApplicationScope private val applicationScope: CoroutineScope,
	@IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : TunnelProvider {

	val appSettings: StateFlow<AppSettings?> = appDataRepository.settings.flow.stateIn(
		scope = applicationScope.plus(ioDispatcher),
		started = SharingStarted.Eagerly,
		initialValue = null,
	)

	@OptIn(ExperimentalCoroutinesApi::class)
	override val activeTunnels = appSettings
		.filterNotNull()
		.flatMapLatest { settings ->
			if (settings.isKernelEnabled) {
				kernelTunnel.activeTunnels
			} else {
				userspaceTunnel.activeTunnels
			}
		}
		.stateIn(
			scope = applicationScope,
			started = SharingStarted.Eagerly,
			initialValue = emptyMap(),
		)

	override suspend fun startTunnel(tunnelConf: TunnelConf) {
		appSettings.withData {
			if (it.isKernelEnabled) return@withData kernelTunnel.startTunnel(tunnelConf)
			userspaceTunnel.startTunnel(tunnelConf)
		}
	}

	override suspend fun stopTunnel(tunnelConf: TunnelConf?) {
		appSettings.withData {
			if (it.isKernelEnabled) return@withData kernelTunnel.stopTunnel(tunnelConf)
			userspaceTunnel.stopTunnel(tunnelConf)
		}
	}

	override suspend fun bounceTunnel(tunnelConf: TunnelConf) {
		appSettings.withData {
			if (it.isKernelEnabled) return@withData kernelTunnel.stopTunnel(tunnelConf)
			userspaceTunnel.stopTunnel(tunnelConf)
		}
	}

	override suspend fun setBackendState(backendState: BackendState, allowedIps: Collection<String>) {
		appSettings.withData {
			if (it.isKernelEnabled) return@withData kernelTunnel.setBackendState(backendState, allowedIps)
			userspaceTunnel.setBackendState(backendState, allowedIps)
		}
	}

	override suspend fun runningTunnelNames(): Set<String> {
		appSettings.filterNotNull().first().let {
			if (it.isKernelEnabled) return kernelTunnel.runningTunnelNames()
			return userspaceTunnel.runningTunnelNames()
		}
	}

	suspend fun restorePreviousState() {
		withContext(ioDispatcher) {
			with(appDataRepository.settings.get()) {
				if (isRestoreOnBootEnabled) {
					val previouslyActiveTuns = appDataRepository.tunnels.getActive()
					// handle kernel mode
					val tunsToStart = previouslyActiveTuns.filterNot { tun -> activeTunnels.value.any { tun.id == it.key } }
					if (isKernelEnabled) {
						return@withContext tunsToStart.forEach {
							startTunnel(it)
						}
					}
					// handle userspace
					if (activeTunnels.value.isEmpty()) tunsToStart.firstOrNull()?.let { startTunnel(it) }
				}
			}
		}
	}
}
