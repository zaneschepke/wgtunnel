package com.zaneschepke.wireguardautotunnel.service.foreground.autotunnel

import com.zaneschepke.wireguardautotunnel.data.domain.TunnelConfig

sealed class AutoTunnelEvent {
	data class Start(val tunnelConfig: TunnelConfig? = null) : AutoTunnelEvent()
	data object Stop : AutoTunnelEvent()
	data object DoNothing : AutoTunnelEvent()
}
