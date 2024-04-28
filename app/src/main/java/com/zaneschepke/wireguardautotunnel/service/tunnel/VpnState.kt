package com.zaneschepke.wireguardautotunnel.service.tunnel

import com.zaneschepke.wireguardautotunnel.data.model.TunnelConfig
import org.amnezia.awg.backend.Statistics
import org.amnezia.awg.backend.Tunnel

data class VpnState(
    val status: Tunnel.State = Tunnel.State.DOWN,
    val tunnelConfig: TunnelConfig? = null,
    val statistics: Statistics? = null
)
