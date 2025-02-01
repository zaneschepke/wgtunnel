package com.zaneschepke.wireguardautotunnel.ui

import com.zaneschepke.wireguardautotunnel.data.domain.GeneralState
import com.zaneschepke.wireguardautotunnel.data.domain.Settings
import com.zaneschepke.wireguardautotunnel.data.domain.TunnelConfig

data class AppUiState(
	val settings: Settings = Settings(),
	val tunnels: List<TunnelConfig> = emptyList(),
	val generalState: GeneralState = GeneralState(),
	val autoTunnelActive: Boolean = false,
)
