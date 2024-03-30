package com.zaneschepke.wireguardautotunnel.service.tunnel

import com.wireguard.android.backend.Statistics
import com.wireguard.android.backend.Tunnel
import com.zaneschepke.wireguardautotunnel.data.model.TunnelConfig

data class VpnState(
    val status: Tunnel.State = Tunnel.State.DOWN,
    val tunnelConfig: TunnelConfig? = null,
    val statistics: Statistics? = null
)
