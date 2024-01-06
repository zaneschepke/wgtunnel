package com.zaneschepke.wireguardautotunnel.ui.screens.settings

import com.zaneschepke.wireguardautotunnel.data.model.Settings
import com.zaneschepke.wireguardautotunnel.data.model.TunnelConfig
import com.zaneschepke.wireguardautotunnel.service.tunnel.VpnState

data class SettingsUiState(
    val settings: Settings = Settings(),
    val tunnels: List<TunnelConfig> = emptyList(),
    val vpnState: VpnState = VpnState(),
    val isLocationDisclosureShown: Boolean = true,
    val isBatteryOptimizeDisableShown: Boolean = false,
    val loading: Boolean = true
)
