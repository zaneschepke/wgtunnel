package com.zaneschepke.wireguardautotunnel.ui

import com.zaneschepke.wireguardautotunnel.data.domain.GeneralState
import com.zaneschepke.wireguardautotunnel.data.domain.Settings
import com.zaneschepke.wireguardautotunnel.data.domain.TunnelConfig
import com.zaneschepke.wireguardautotunnel.service.tunnel.VpnState

data class AppUiState(
	val settings: Settings = Settings(),
	val tunnels: List<TunnelConfig> = emptyList(),
	val vpnState: VpnState = VpnState(),
	val generalState: GeneralState = GeneralState(),
	val isKernelAvailable: Boolean = false,
	val isRooted: Boolean = false,
)
