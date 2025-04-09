package com.zaneschepke.wireguardautotunnel.domain.state

import com.zaneschepke.wireguardautotunnel.domain.enums.BackendState
import com.zaneschepke.wireguardautotunnel.domain.enums.TunnelStatus

data class TunnelState(
    val status: TunnelStatus = TunnelStatus.Down,
    val backendState: BackendState = BackendState.INACTIVE,
    val statistics: TunnelStatistics? = null,
)
