package com.zaneschepke.wireguardautotunnel.core.tunnel

import com.zaneschepke.wireguardautotunnel.domain.entity.TunnelConf
import com.zaneschepke.wireguardautotunnel.domain.enums.BackendState
import com.zaneschepke.wireguardautotunnel.domain.enums.TunnelStatus
import com.zaneschepke.wireguardautotunnel.domain.state.TunnelState
import com.zaneschepke.wireguardautotunnel.domain.state.TunnelStatistics
import kotlinx.coroutines.flow.StateFlow

interface TunnelProvider {
	suspend fun startTunnel(tunnelConf: TunnelConf)
	suspend fun stopTunnel(tunnelConf: TunnelConf? = null, reason: TunnelStatus.StopReason = TunnelStatus.StopReason.USER)
	suspend fun bounceTunnel(tunnelConf: TunnelConf, reason: TunnelStatus.StopReason = TunnelStatus.StopReason.USER)
	fun setBackendState(backendState: BackendState, allowedIps: Collection<String>)
	fun getBackendState(): BackendState
	suspend fun runningTunnelNames(): Set<String>
	fun getStatistics(tunnelConf: TunnelConf): TunnelStatistics?
	val activeTunnels: StateFlow<Map<TunnelConf, TunnelState>>
	fun hasVpnPermission(): Boolean
	suspend fun clearError(tunnelConf: TunnelConf)
	suspend fun updateTunnelStatistics(tunnel: TunnelConf)
}
