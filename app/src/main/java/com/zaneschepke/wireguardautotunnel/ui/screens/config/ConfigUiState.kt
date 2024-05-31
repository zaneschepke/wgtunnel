package com.zaneschepke.wireguardautotunnel.ui.screens.config

import com.wireguard.config.Config
import com.zaneschepke.wireguardautotunnel.data.domain.TunnelConfig
import com.zaneschepke.wireguardautotunnel.ui.screens.config.model.InterfaceProxy
import com.zaneschepke.wireguardautotunnel.ui.screens.config.model.PeerProxy
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
    val tunnelName: String = "",
    val isAmneziaEnabled: Boolean = false
) {
    companion object {
        fun from(config: Config): ConfigUiState {
            val proxyPeers = config.peers.map { PeerProxy.from(it) }
            val proxyInterface = InterfaceProxy.from(config.`interface`)
            var include = true
            var isAllApplicationsEnabled = false
            val checkedPackages =
                if (config.`interface`.includedApplications.isNotEmpty()) {
                    config.`interface`.includedApplications
                } else if (config.`interface`.excludedApplications.isNotEmpty()) {
                    include = false
                    config.`interface`.excludedApplications
                } else {
                    isAllApplicationsEnabled = true
                    emptySet()
                }
            return ConfigUiState(
                proxyPeers,
                proxyInterface,
                emptyList(),
                checkedPackages.toList(),
                include,
                isAllApplicationsEnabled,
            )
        }

        fun from(config: org.amnezia.awg.config.Config): ConfigUiState {
            //TODO update with new values
            val proxyPeers = config.peers.map { PeerProxy.from(it) }
            val proxyInterface = InterfaceProxy.from(config.`interface`)
            var include = true
            var isAllApplicationsEnabled = false
            val checkedPackages =
                if (config.`interface`.includedApplications.isNotEmpty()) {
                    config.`interface`.includedApplications
                } else if (config.`interface`.excludedApplications.isNotEmpty()) {
                    include = false
                    config.`interface`.excludedApplications
                } else {
                    isAllApplicationsEnabled = true
                    emptySet()
                }
            return ConfigUiState(
                proxyPeers,
                proxyInterface,
                emptyList(),
                checkedPackages.toList(),
                include,
                isAllApplicationsEnabled,
            )
        }
    }
}
