package com.zaneschepke.wireguardautotunnel.service.foreground.autotunnel

import com.zaneschepke.wireguardautotunnel.data.domain.TunnelConfig

sealed class AutoTunnelEvent {
	data class Start(val tunnelConfig: TunnelConfig? = null) : AutoTunnelEvent()
	data class Stop(val tunnelConfig: TunnelConfig?) : AutoTunnelEvent()
	data object DoNothing : AutoTunnelEvent()
}
