package com.zaneschepke.wireguardautotunnel.service.tunnel

import com.wireguard.android.backend.Tunnel
import com.zaneschepke.wireguardautotunnel.data.domain.TunnelConfig
import kotlinx.coroutines.flow.StateFlow

interface TunnelService : Tunnel, org.amnezia.awg.backend.Tunnel {
	suspend fun startTunnel(tunnelConfig: TunnelConfig): Result<TunnelState>

	suspend fun stopTunnel(tunnelConfig: TunnelConfig): Result<TunnelState>

	val vpnState: StateFlow<VpnState>

	suspend fun runningTunnelNames(): Set<String>

	suspend fun getState(): TunnelState

	fun cancelStatsJob()
	fun startStatsJob()
}
