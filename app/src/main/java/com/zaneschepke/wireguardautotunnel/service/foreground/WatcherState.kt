package com.zaneschepke.wireguardautotunnel.service.foreground

import com.zaneschepke.wireguardautotunnel.data.domain.Settings

data class WatcherState(
	val isWifiConnected: Boolean = false,
	val isEthernetConnected: Boolean = false,
	val isMobileDataConnected: Boolean = false,
	val currentNetworkSSID: String = "",
	val settings: Settings = Settings(),
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
				!settings.trustedNetworkSSIDs.contains(currentNetworkSSID) &&
				settings.isTunnelOnWifiEnabled
			)
	}

	fun isTrustedWifiConditionMet(): Boolean {
		return (
			!isEthernetConnected &&
				(
					isWifiConnected &&
						settings.trustedNetworkSSIDs.contains(currentNetworkSSID)
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
}
