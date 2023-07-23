package com.zaneschepke.wireguardautotunnel.service.tunnel.model

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id

@Entity
data class Settings(
    @Id
    var id : Long = 0,
    var isAutoTunnelEnabled : Boolean = false,
    var isTunnelOnMobileDataEnabled : Boolean = false,
    var trustedNetworkSSIDs : MutableList<String> = mutableListOf(),
    var defaultTunnel : String? = null,
    var isAlwaysOnVpnEnabled : Boolean = false
)
