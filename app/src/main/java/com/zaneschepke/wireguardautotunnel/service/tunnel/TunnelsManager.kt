package com.zaneschepke.wireguardautotunnel.service.tunnel

import com.zaneschepke.wireguardautotunnel.data.domain.TunnelConfig
import com.zaneschepke.wireguardautotunnel.service.tunnel.model.VpnState
import kotlinx.coroutines.flow.Flow

interface TunnelsManager {
	val tunnelStates : Flow<Map<Int, VpnState>>
	suspend fun getTunnel(tunnelConfig: TunnelConfig, isKernelTunnel: Boolean) : TunnelService
}
