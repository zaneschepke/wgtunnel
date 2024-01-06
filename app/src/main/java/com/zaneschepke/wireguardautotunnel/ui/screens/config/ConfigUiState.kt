package com.zaneschepke.wireguardautotunnel.ui.screens.config

import com.zaneschepke.wireguardautotunnel.data.model.TunnelConfig
import com.zaneschepke.wireguardautotunnel.ui.models.InterfaceProxy
import com.zaneschepke.wireguardautotunnel.ui.models.PeerProxy
import com.zaneschepke.wireguardautotunnel.util.Packages

data class ConfigUiState(
    val proxyPeers: List<PeerProxy> = arrayListOf(PeerProxy()),
    val interfaceProxy: InterfaceProxy = InterfaceProxy(),
    val packages: Packages = emptyList(),
    val checkedPackageNames: List<String> = emptyList(),
    val include: Boolean = true,
    val isAllApplicationsEnabled: Boolean = false,
    val loading: Boolean = true,
    val tunnel: TunnelConfig? = null,
    val tunnelName: String = ""
)
