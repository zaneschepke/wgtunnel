package com.zaneschepke.wireguardautotunnel.service.tunnel.model

import com.zaneschepke.wireguardautotunnel.service.tunnel.statistics.TunnelStatistics

data class VpnState(
	val state: TunnelState = TunnelState.DOWN,
	val backendState: BackendState = BackendState.INACTIVE,
	val statistics: TunnelStatistics? = null,
)
