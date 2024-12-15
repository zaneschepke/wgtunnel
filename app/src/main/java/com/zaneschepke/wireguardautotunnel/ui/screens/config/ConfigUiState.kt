package com.zaneschepke.wireguardautotunnel.ui.screens.config

import com.zaneschepke.wireguardautotunnel.data.domain.TunnelConfig
import com.zaneschepke.wireguardautotunnel.ui.screens.config.model.InterfaceProxy
import com.zaneschepke.wireguardautotunnel.ui.screens.config.model.PeerProxy

data class ConfigUiState(
	val proxyPeers: List<PeerProxy> = arrayListOf(PeerProxy()),
	val interfaceProxy: InterfaceProxy = InterfaceProxy(),
	var tunnelName: String = "",
) {
	companion object {
		private fun createProxyPair(config: org.amnezia.awg.config.Config): Pair<InterfaceProxy, List<PeerProxy>> {
			val proxyPeers = config.peers.map { PeerProxy.from(it) }
			val proxyInterface = InterfaceProxy.from(config.`interface`)
			return Pair(proxyInterface, proxyPeers)
		}

		fun from(tunnel: TunnelConfig): ConfigUiState {
			val config = tunnel.toAmConfig()
			val configPair = createProxyPair(config)
			return ConfigUiState(
				tunnelName = tunnel.name,
				proxyPeers = configPair.second,
				interfaceProxy = configPair.first
			)
		}
	}
}
