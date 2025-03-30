package com.zaneschepke.wireguardautotunnel.core.tunnel

import com.zaneschepke.wireguardautotunnel.domain.entity.TunnelConf
import com.zaneschepke.wireguardautotunnel.domain.enums.BackendState
import com.zaneschepke.wireguardautotunnel.domain.state.TunnelState
import com.zaneschepke.wireguardautotunnel.domain.state.TunnelStatistics
import kotlinx.coroutines.flow.StateFlow

interface TunnelProvider {
	fun startTunnel(tunnelConf: TunnelConf)
	fun stopTunnel(tunnelConf: TunnelConf? = null)
	fun bounceTunnel(tunnelConf: TunnelConf)
	suspend fun setBackendState(backendState: BackendState, allowedIps: Collection<String>)
	suspend fun runningTunnelNames(): Set<String>
	fun getStatistics(tunnelConf: TunnelConf): TunnelStatistics?
	val activeTunnels: StateFlow<Map<TunnelConf, TunnelState>>
}
