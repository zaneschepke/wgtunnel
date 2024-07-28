package com.zaneschepke.wireguardautotunnel.service.tunnel

import com.wireguard.android.backend.Tunnel
import com.zaneschepke.wireguardautotunnel.data.domain.TunnelConfig
import kotlinx.coroutines.flow.StateFlow

interface VpnService : Tunnel, org.amnezia.awg.backend.Tunnel {
	suspend fun startTunnel(tunnelConfig: TunnelConfig? = null): TunnelState

	suspend fun stopTunnel()

	val vpnState: StateFlow<VpnState>

	fun getState(): TunnelState
}
