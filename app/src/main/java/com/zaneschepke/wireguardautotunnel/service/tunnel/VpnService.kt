package com.zaneschepke.wireguardautotunnel.service.tunnel

import com.zaneschepke.wireguardautotunnel.data.model.TunnelConfig
import kotlinx.coroutines.flow.StateFlow
import org.amnezia.awg.backend.Tunnel

interface VpnService : Tunnel {
    suspend fun startTunnel(tunnelConfig: TunnelConfig? = null): Tunnel.State

    suspend fun stopTunnel()

    val vpnState: StateFlow<VpnState>

    fun getState(): Tunnel.State
}
