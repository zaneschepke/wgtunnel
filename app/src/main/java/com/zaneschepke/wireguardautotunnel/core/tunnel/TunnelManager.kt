package com.zaneschepke.wireguardautotunnel.core.tunnel

import com.zaneschepke.wireguardautotunnel.di.ApplicationScope
import com.zaneschepke.wireguardautotunnel.di.IoDispatcher
import com.zaneschepke.wireguardautotunnel.di.Kernel
import com.zaneschepke.wireguardautotunnel.di.Userspace
import com.zaneschepke.wireguardautotunnel.domain.enums.BackendError
import com.zaneschepke.wireguardautotunnel.domain.enums.BackendState
import com.zaneschepke.wireguardautotunnel.domain.enums.TunnelStatus
import com.zaneschepke.wireguardautotunnel.domain.model.TunnelConf
import com.zaneschepke.wireguardautotunnel.domain.repository.AppDataRepository
import com.zaneschepke.wireguardautotunnel.domain.state.TunnelStatistics
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus

class TunnelManager
@Inject
constructor(
    @Kernel private val kernelTunnel: TunnelProvider,
    @Userspace private val userspaceTunnel: TunnelProvider,
    private val appDataRepository: AppDataRepository,
    @ApplicationScope private val applicationScope: CoroutineScope,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : TunnelProvider {

    @OptIn(ExperimentalCoroutinesApi::class)
    private val tunnelProviderFlow =
        appDataRepository.settings.flow
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
    override val activeTunnels =
        appDataRepository.settings.flow
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

    override val errorEvents: SharedFlow<Pair<TunnelConf, BackendError>>
        get() = tunnelProviderFlow.value.errorEvents

    override val bouncingTunnelIds: ConcurrentHashMap<Int, TunnelStatus.StopReason> =
        tunnelProviderFlow.value.bouncingTunnelIds

    override fun hasVpnPermission(): Boolean {
        return userspaceTunnel.hasVpnPermission()
    }

    override suspend fun updateTunnelStatistics(tunnel: TunnelConf) {
        tunnelProviderFlow.value.updateTunnelStatistics(tunnel)
    }

    override suspend fun startTunnel(tunnelConf: TunnelConf) {
        tunnelProviderFlow.value.startTunnel(tunnelConf)
    }

    override suspend fun stopTunnel(tunnelConf: TunnelConf?, reason: TunnelStatus.StopReason) {
        tunnelProviderFlow.value.stopTunnel(tunnelConf, reason)
    }

    override suspend fun bounceTunnel(tunnelConf: TunnelConf, reason: TunnelStatus.StopReason) {
        tunnelProviderFlow.value.bounceTunnel(tunnelConf, reason)
    }

    override fun setBackendState(backendState: BackendState, allowedIps: Collection<String>) {
        tunnelProviderFlow.value.setBackendState(backendState, allowedIps)
    }

    override fun getBackendState(): BackendState {
        return tunnelProviderFlow.value.getBackendState()
    }

    override suspend fun runningTunnelNames(): Set<String> {
        return tunnelProviderFlow.value.runningTunnelNames()
    }

    override fun getStatistics(tunnelConf: TunnelConf): TunnelStatistics? {
        return tunnelProviderFlow.value.getStatistics(tunnelConf)
    }

    fun restorePreviousState() =
        applicationScope.launch(ioDispatcher) {
            val settings = appDataRepository.settings.get()
            if (settings.isRestoreOnBootEnabled) {
                val previouslyActiveTuns = appDataRepository.tunnels.getActive()
                val tunsToStart =
                    previouslyActiveTuns.filterNot { tun ->
                        activeTunnels.value.any { tun.id == it.key.id }
                    }
                if (settings.isKernelEnabled) {
                    return@launch tunsToStart.forEach { startTunnel(it) }
                } else {
                    tunsToStart.firstOrNull()?.let { startTunnel(it) }
                }
            }
        }
}
