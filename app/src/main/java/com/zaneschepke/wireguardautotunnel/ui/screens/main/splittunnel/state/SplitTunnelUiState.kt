package com.zaneschepke.wireguardautotunnel.ui.screens.main.splittunnel.state

import com.zaneschepke.wireguardautotunnel.domain.model.TunnelConf

data class SplitTunnelUiState(
    val loading: Boolean = true,
    val tunnelConf: TunnelConf? = null,
    val tunneledApps: SplitTunnelApps = emptyList(),
    val queriedApps: SplitTunnelApps = emptyList(),
    val splitOption: SplitOption = SplitOption.ALL,
    val searchQuery: String = "",
    val success: Boolean? = null,
)
