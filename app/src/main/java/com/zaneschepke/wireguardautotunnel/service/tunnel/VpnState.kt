package com.zaneschepke.wireguardautotunnel.service.tunnel

import com.zaneschepke.wireguardautotunnel.data.domain.TunnelConfig
import com.zaneschepke.wireguardautotunnel.service.tunnel.statistics.TunnelStatistics

data class VpnState(
    val status: TunnelState = TunnelState.DOWN,
    val tunnelConfig: TunnelConfig? = null,
    val statistics: TunnelStatistics? = null
)
