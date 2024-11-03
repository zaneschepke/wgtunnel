package com.zaneschepke.wireguardautotunnel.service.foreground

import com.zaneschepke.wireguardautotunnel.data.domain.Settings
import com.zaneschepke.wireguardautotunnel.data.domain.TunnelConfig
import com.zaneschepke.wireguardautotunnel.service.tunnel.VpnState
import com.zaneschepke.wireguardautotunnel.util.extensions.TunnelConfigs
import com.zaneschepke.wireguardautotunnel.util.extensions.isMatchingToWildcardList

data class AutoTunnelState(
	val vpnState: VpnState = VpnState(),
	val isWifiConnected: Boolean = false,
	val isEthernetConnected: Boolean = false,
	val isMobileDataConnected: Boolean = false,
	val currentNetworkSSID: String = "",
	val settings: Settings = Settings(),
	val tunnels: TunnelConfigs = emptyList(),
) {
	fun isEthernetConditionMet(): Boolean {
		return (
			isEthernetConnected &&
				settings.isTunnelOnEthernetEnabled
			)
	}

	fun isMobileDataConditionMet(): Boolean {
		return (
			!isEthernetConnected &&
				settings.isTunnelOnMobileDataEnabled &&
				!isWifiConnected &&
				isMobileDataConnected
			)
	}

	fun isTunnelOffOnMobileDataConditionMet(): Boolean {
		return (
			!isEthernetConnected &&
				!settings.isTunnelOnMobileDataEnabled &&
				isMobileDataConnected &&
				!isWifiConnected
			)
	}

	fun isUntrustedWifiConditionMet(): Boolean {
		return (
			!isEthernetConnected &&
				isWifiConnected &&
				!isCurrentSSIDTrusted() &&
				settings.isTunnelOnWifiEnabled
			)
	}

	fun isTrustedWifiConditionMet(): Boolean {
		return (
			!isEthernetConnected &&
				(
					isWifiConnected &&
						isCurrentSSIDTrusted()
					)
			)
	}

	fun isTunnelOffOnWifiConditionMet(): Boolean {
		return (
			!isEthernetConnected &&
				(
					isWifiConnected &&
						!settings.isTunnelOnWifiEnabled
					)
			)
	}

	fun isTunnelOffOnNoConnectivityMet(): Boolean {
		return (
			!isEthernetConnected &&
				!isWifiConnected &&
				!isMobileDataConnected
			)
	}

	fun isCurrentSSIDTrusted(): Boolean {
		return if (settings.isWildcardsEnabled) {
			settings.trustedNetworkSSIDs.isMatchingToWildcardList(currentNetworkSSID)
		} else {
			settings.trustedNetworkSSIDs.contains(currentNetworkSSID)
		}
	}
	fun isCurrentSSIDActiveTunnelNetwork(): Boolean {
		val currentTunnelNetworks = vpnState.tunnelConfig?.tunnelNetworks
		return (
			if (settings.isWildcardsEnabled) {
				currentTunnelNetworks?.isMatchingToWildcardList(currentNetworkSSID)
			} else {
				currentTunnelNetworks?.contains(currentNetworkSSID)
			}
			) == true
	}

	fun getTunnelWithMatchingTunnelNetwork(): TunnelConfig? {
		return tunnels.firstOrNull {
			if (settings.isWildcardsEnabled) {
				it.tunnelNetworks.isMatchingToWildcardList(currentNetworkSSID)
			} else {
				it.tunnelNetworks.contains(currentNetworkSSID)
			}
		}
	}
}
