package com.zaneschepke.wireguardautotunnel.service.foreground.autotunnel

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

	private fun isMobileDataActive(): Boolean {
		return !isEthernetConnected && !isWifiConnected && isMobileDataConnected
	}

	private fun isMobileTunnelDataChangeNeeded(): Boolean {
		val preferredTunnel = preferredMobileDataTunnel()
		return preferredTunnel != null &&
			vpnState.status.isUp() && preferredTunnel.id != vpnState.tunnelConfig?.id
	}

	private fun isEthernetTunnelChangeNeeded(): Boolean {
		val preferredTunnel = preferredEthernetTunnel()
		return preferredTunnel != null && vpnState.status.isUp() && preferredTunnel.id != vpnState.tunnelConfig?.id
	}

	private fun preferredMobileDataTunnel(): TunnelConfig? {
		return tunnels.firstOrNull { it.isMobileDataTunnel } ?: tunnels.firstOrNull { it.isPrimaryTunnel }
	}

	private fun preferredEthernetTunnel(): TunnelConfig? {
		return tunnels.firstOrNull { it.isEthernetTunnel } ?: tunnels.firstOrNull { it.isPrimaryTunnel }
	}

	private fun preferredWifiTunnel(): TunnelConfig? {
		return getTunnelWithMatchingTunnelNetwork() ?: tunnels.firstOrNull { it.isPrimaryTunnel }
	}

	private fun isWifiActive(): Boolean {
		return !isEthernetConnected && isWifiConnected
	}

	private fun startOnEthernet(): Boolean {
		return isEthernetConnected && settings.isTunnelOnEthernetEnabled && vpnState.status.isDown()
	}

	private fun stopOnEthernet(): Boolean {
		return isEthernetConnected && !settings.isTunnelOnEthernetEnabled && vpnState.status.isUp()
	}

	fun isNoConnectivity(): Boolean {
		return !isEthernetConnected && !isWifiConnected && !isMobileDataConnected
	}

	private fun stopOnMobileData(): Boolean {
		return isMobileDataActive() && !settings.isTunnelOnMobileDataEnabled && vpnState.status.isUp()
	}

	private fun startOnMobileData(): Boolean {
		return isMobileDataActive() && settings.isTunnelOnMobileDataEnabled && vpnState.status.isDown()
	}

	private fun changeOnMobileData(): Boolean {
		return isMobileDataActive() && settings.isTunnelOnMobileDataEnabled && isMobileTunnelDataChangeNeeded()
	}

	private fun changeOnEthernet(): Boolean {
		return isEthernetConnected && settings.isTunnelOnEthernetEnabled && isEthernetTunnelChangeNeeded()
	}

	private fun stopOnWifi(): Boolean {
		return isWifiActive() && !settings.isTunnelOnWifiEnabled && vpnState.status.isUp()
	}

	private fun stopOnTrustedWifi(): Boolean {
		return isWifiActive() && settings.isTunnelOnWifiEnabled && vpnState.status.isUp() && isCurrentSSIDTrusted()
	}

	private fun startOnUntrustedWifi(): Boolean {
		return isWifiActive() && settings.isTunnelOnWifiEnabled && vpnState.status.isDown() && !isCurrentSSIDTrusted()
	}

	private fun changeOnUntrustedWifi(): Boolean {
		return isWifiActive() && settings.isTunnelOnWifiEnabled && vpnState.status.isUp() && !isCurrentSSIDTrusted() && !isWifiTunnelPreferred()
	}

	private fun isWifiTunnelPreferred(): Boolean {
		val preferred = preferredWifiTunnel()
		val vpnTunnel = vpnState.tunnelConfig
		return if (preferred != null && vpnTunnel != null) {
			preferred.id == vpnTunnel.id
		} else {
			true
		}
	}

	fun asAutoTunnelEvent(): AutoTunnelEvent {
		return when {
			// ethernet scenarios
			stopOnEthernet() -> AutoTunnelEvent.Stop
			startOnEthernet() || changeOnEthernet() -> AutoTunnelEvent.Start(preferredEthernetTunnel())
			// mobile data scenarios
			stopOnMobileData() -> AutoTunnelEvent.Stop
			startOnMobileData() || changeOnMobileData() -> AutoTunnelEvent.Start(preferredMobileDataTunnel())
			// wifi scenarios
			stopOnWifi() -> AutoTunnelEvent.Stop
			stopOnTrustedWifi() -> AutoTunnelEvent.Stop
			startOnUntrustedWifi() || changeOnUntrustedWifi() -> AutoTunnelEvent.Start(preferredWifiTunnel())
			// no connectivity
			isNoConnectivity() && settings.isStopOnNoInternetEnabled -> AutoTunnelEvent.Stop
			else -> AutoTunnelEvent.DoNothing
		}
	}

	private fun isCurrentSSIDTrusted(): Boolean {
		return if (settings.isWildcardsEnabled) {
			settings.trustedNetworkSSIDs.isMatchingToWildcardList(currentNetworkSSID)
		} else {
			settings.trustedNetworkSSIDs.contains(currentNetworkSSID)
		}
	}

	private fun getTunnelWithMatchingTunnelNetwork(): TunnelConfig? {
		return tunnels.firstOrNull {
			if (settings.isWildcardsEnabled) {
				it.tunnelNetworks.isMatchingToWildcardList(currentNetworkSSID)
			} else {
				it.tunnelNetworks.contains(currentNetworkSSID)
			}
		}
	}

	fun isPingEnabled(): Boolean {
		return settings.isPingEnabled ||
			(vpnState.status.isUp() && vpnState.tunnelConfig != null && tunnels.first { it.id == vpnState.tunnelConfig.id }.isPingEnabled)
	}
}
