package com.zaneschepke.wireguardautotunnel.core.tunnel

import com.zaneschepke.wireguardautotunnel.di.ApplicationScope
import com.zaneschepke.wireguardautotunnel.di.IoDispatcher
import com.zaneschepke.wireguardautotunnel.di.Kernel
import com.zaneschepke.wireguardautotunnel.di.Userspace
import com.zaneschepke.wireguardautotunnel.domain.entity.TunnelConf
import com.zaneschepke.wireguardautotunnel.domain.enums.BackendState
import com.zaneschepke.wireguardautotunnel.domain.repository.AppDataRepository
import com.zaneschepke.wireguardautotunnel.domain.state.TunnelStatistics
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import javax.inject.Inject

class TunnelManager @Inject constructor(
	@Kernel private val kernelTunnel: TunnelProvider,
	@Userspace private val userspaceTunnel: TunnelProvider,
	private val appDataRepository: AppDataRepository,
	@ApplicationScope private val applicationScope: CoroutineScope,
	@IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : TunnelProvider {

	@OptIn(ExperimentalCoroutinesApi::class)
	private val tunnelProviderFlow = appDataRepository.settings.flow
		.filterNotNull()
		.flatMapLatest { settings ->
			MutableStateFlow(if (settings.isKernelEnabled) kernelTunnel else userspaceTunnel)
		}
		.stateIn(
			scope = applicationScope.plus(ioDispatcher),
			started = SharingStarted.Eagerly,
			initialValue = userspaceTunnel,
		)

	@OptIn(ExperimentalCoroutinesApi::class)
	override val activeTunnels = appDataRepository.settings.flow
		.filterNotNull()
		.flatMapLatest { settings ->
			if (settings.isKernelEnabled) {
				kernelTunnel.activeTunnels
			} else {
				userspaceTunnel.activeTunnels
			}
		}
		.stateIn(
			scope = applicationScope.plus(ioDispatcher),
			started = SharingStarted.Eagerly,
			initialValue = emptyMap(),
		)

	override fun startTunnel(tunnelConf: TunnelConf) {
		tunnelProviderFlow.value.startTunnel(tunnelConf)
	}

	override fun stopTunnel(tunnelConf: TunnelConf?) {
		tunnelProviderFlow.value.stopTunnel(tunnelConf)
	}

	override fun bounceTunnel(tunnelConf: TunnelConf) {
		tunnelProviderFlow.value.bounceTunnel(tunnelConf)
	}

	override suspend fun setBackendState(backendState: BackendState, allowedIps: Collection<String>) {
		tunnelProviderFlow.value.setBackendState(backendState, allowedIps)
	}

	override suspend fun runningTunnelNames(): Set<String> {
		return tunnelProviderFlow.value.runningTunnelNames()
	}

	override fun getStatistics(tunnelConf: TunnelConf): TunnelStatistics? {
		return tunnelProviderFlow.value.getStatistics(tunnelConf)
	}

	fun restorePreviousState() = applicationScope.launch(ioDispatcher) {
		val settings = appDataRepository.settings.get()
		if (settings.isRestoreOnBootEnabled) {
			val previouslyActiveTuns = appDataRepository.tunnels.getActive()
			val tunsToStart = previouslyActiveTuns.filterNot { tun -> activeTunnels.value.any { tun.id == it.key.id } }
			if (settings.isKernelEnabled) {
				return@launch tunsToStart.forEach {
					startTunnel(it)
				}
			} else {
				tunsToStart.firstOrNull()?.let { startTunnel(it) }
			}
		}
	}
}
