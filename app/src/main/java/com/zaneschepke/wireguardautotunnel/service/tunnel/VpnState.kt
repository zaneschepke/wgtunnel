package com.zaneschepke.wireguardautotunnel.service.tunnel

import com.wireguard.android.backend.Statistics
import com.wireguard.android.backend.Tunnel

data class VpnState(
    val status : Tunnel.State = Tunnel.State.DOWN,
    val name : String = "",
    val statistics : Statistics? = null
)
