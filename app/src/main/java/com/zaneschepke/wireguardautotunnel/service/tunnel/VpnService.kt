package com.zaneschepke.wireguardautotunnel.service.tunnel

import com.wireguard.android.backend.Tunnel
import com.zaneschepke.wireguardautotunnel.service.tunnel.model.TunnelConfig
import kotlinx.coroutines.flow.SharedFlow

interface VpnService : Tunnel {
    suspend fun startTunnel(tunnelConfig : TunnelConfig) : Tunnel.State
    suspend fun stopTunnel()
    val state : SharedFlow<Tunnel.State>
    val tunnelName : SharedFlow<String>
    fun getState() : Tunnel.State
}