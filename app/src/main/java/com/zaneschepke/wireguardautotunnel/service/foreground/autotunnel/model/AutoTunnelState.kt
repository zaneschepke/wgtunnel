package com.zaneschepke.wireguardautotunnel.service.foreground.autotunnel.model

import com.zaneschepke.wireguardautotunnel.data.domain.Settings
import com.zaneschepke.wireguardautotunnel.data.domain.TunnelConfig
import com.zaneschepke.wireguardautotunnel.service.tunnel.BackendState
import com.zaneschepke.wireguardautotunnel.service.tunnel.VpnState
import com.zaneschepke.wireguardautotunnel.util.extensions.TunnelConfigs
import com.zaneschepke.wireguardautotunnel.util.extensions.isMatchingToWildcardList

data class AutoTunnelState(
	val vpnState: VpnState = VpnState(),
	val networkState: NetworkState = NetworkState(),
	val settings: Settings = Settings(),
	val tunnels: TunnelConfigs = emptyList(),
) {

	private fun isMobileDataActive(): Boolean {
		return !networkState.isEthernetConnected && !networkState.isWifiConnected && networkState.isMobileDataConnected
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
		return !networkState.isEthernetConnected && networkState.isWifiConnected
	}

	private fun startOnEthernet(): Boolean {
		return networkState.isEthernetConnected && settings.isTunnelOnEthernetEnabled && vpnState.status.isDown()
	}

	private fun stopOnEthernet(): Boolean {
		return networkState.isEthernetConnected && !settings.isTunnelOnEthernetEnabled && vpnState.status.isUp()
	}

	private fun stopKillSwitchOnTrusted(): Boolean {
		return networkState.isWifiConnected && settings.isVpnKillSwitchEnabled && settings.isDisableKillSwitchOnTrustedEnabled && isCurrentSSIDTrusted() && vpnState.backendState == BackendState.KILL_SWITCH_ACTIVE
	}

	private fun startKillSwitch(): Boolean {
		return settings.isVpnKillSwitchEnabled && vpnState.backendState != BackendState.KILL_SWITCH_ACTIVE && (!settings.isDisableKillSwitchOnTrustedEnabled || !isCurrentSSIDTrusted())
	}

	fun isNoConnectivity(): Boolean {
		return !networkState.isEthernetConnected && !networkState.isWifiConnected && !networkState.isMobileDataConnected
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
		return networkState.isEthernetConnected && settings.isTunnelOnEthernetEnabled && isEthernetTunnelChangeNeeded()
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

	fun asKillSwitchEvent(): KillSwitchEvent {
		return when {
			stopKillSwitchOnTrusted() -> KillSwitchEvent.Stop
			startKillSwitch() -> {
				val allowedIps = if (settings.isLanOnKillSwitchEnabled) TunnelConfig.LAN_BYPASS_ALLOWED_IPS else emptySet()
				KillSwitchEvent.Start(allowedIps)
			}
			else -> KillSwitchEvent.DoNothing
		}
	}

	private fun isCurrentSSIDTrusted(): Boolean {
		return networkState.wifiName?.let {
			hasTrustedWifiName(it)
		} == true
	}

	private fun hasTrustedWifiName(wifiName: String, wifiNames: List<String> = settings.trustedNetworkSSIDs): Boolean {
		return if (settings.isWildcardsEnabled) {
			wifiNames.isMatchingToWildcardList(wifiName)
		} else {
			wifiNames.contains(wifiName)
		}
	}

	private fun getTunnelWithMatchingTunnelNetwork(): TunnelConfig? {
		return networkState.wifiName?.let { wifiName ->
			tunnels.firstOrNull {
				hasTrustedWifiName(wifiName, it.tunnelNetworks)
			}
		}
	}

	fun isPingEnabled(): Boolean {
		return settings.isPingEnabled ||
			(vpnState.status.isUp() && vpnState.tunnelConfig != null && tunnels.first { it.id == vpnState.tunnelConfig.id }.isPingEnabled)
	}
}
