package com.zaneschepke.wireguardautotunnel.service.tunnel

import com.wireguard.android.backend.Statistics
import com.wireguard.android.backend.Tunnel
import com.wireguard.config.Config
import com.zaneschepke.wireguardautotunnel.data.model.TunnelConfig

data class VpnState(
    val status: Tunnel.State = Tunnel.State.DOWN,
    val name: String = "",
    val config: Config? = null,
    val statistics: Statistics? = null
)
