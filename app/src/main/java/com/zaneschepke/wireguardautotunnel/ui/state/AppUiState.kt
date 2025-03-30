package com.zaneschepke.wireguardautotunnel.ui.state

import com.zaneschepke.wireguardautotunnel.data.model.GeneralState
import com.zaneschepke.wireguardautotunnel.domain.entity.AppSettings
import com.zaneschepke.wireguardautotunnel.domain.entity.TunnelConf
import com.zaneschepke.wireguardautotunnel.domain.state.TunnelState

data class AppUiState(
	val appSettings: AppSettings = AppSettings(),
	val tunnels: List<TunnelConf> = emptyList(),
	val activeTunnels: Map<TunnelConf, TunnelState> = emptyMap(),
	val generalState: GeneralState = GeneralState(),
	val autoTunnelActive: Boolean = false,
)
