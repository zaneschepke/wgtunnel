package com.zaneschepke.wireguardautotunnel.ui.state

import com.zaneschepke.networkmonitor.NetworkStatus
import com.zaneschepke.wireguardautotunnel.data.model.GeneralState
import com.zaneschepke.wireguardautotunnel.domain.entity.AppSettings
import com.zaneschepke.wireguardautotunnel.domain.entity.AppState
import com.zaneschepke.wireguardautotunnel.domain.entity.TunnelConf
import com.zaneschepke.wireguardautotunnel.domain.state.TunnelState

data class AppUiState(
	val appSettings: AppSettings = AppSettings(),
	val tunnels: List<TunnelConf> = emptyList(),
	val activeTunnels: Map<TunnelConf, TunnelState> = emptyMap(),
	val appState: AppState = GeneralState().toAppState(),
	val isAutoTunnelActive: Boolean = false,
	val appConfigurationChange: Boolean = false,
	val isAppLoaded: Boolean = false,
	val selectedTunnel: TunnelConf? = null,
	val networkStatus: NetworkStatus? = null,
)
