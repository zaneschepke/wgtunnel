package com.zaneschepke.wireguardautotunnel.service.tunnel

import com.wireguard.android.backend.Tunnel
import com.zaneschepke.wireguardautotunnel.data.domain.TunnelConfig
import kotlinx.coroutines.flow.StateFlow

interface TunnelService : Tunnel, org.amnezia.awg.backend.Tunnel {

	suspend fun startTunnel(tunnelConfig: TunnelConfig?, background: Boolean = false)

	suspend fun stopTunnel()

	suspend fun bounceTunnel()

	suspend fun getBackendState(): BackendState

	suspend fun setBackendState(backendState: BackendState, allowedIps: Collection<String>)

	val vpnState: StateFlow<VpnState>

	suspend fun runningTunnelNames(): Set<String>

	suspend fun getState(): TunnelState

	fun cancelActiveTunnelJobs()

	fun startActiveTunnelJobs()
}
