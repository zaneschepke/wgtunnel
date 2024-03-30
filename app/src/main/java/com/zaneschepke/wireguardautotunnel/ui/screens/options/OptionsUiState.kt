package com.zaneschepke.wireguardautotunnel.ui.screens.options

import com.zaneschepke.wireguardautotunnel.data.model.TunnelConfig

data class OptionsUiState(
    val id: String? = null,
    val tunnel: TunnelConfig? = null,
    val isDefaultTunnel: Boolean = false
)
