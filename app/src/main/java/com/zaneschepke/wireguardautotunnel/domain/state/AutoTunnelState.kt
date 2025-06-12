package com.zaneschepke.wireguardautotunnel.domain.state

import com.zaneschepke.wireguardautotunnel.core.tunnel.allDown
import com.zaneschepke.wireguardautotunnel.core.tunnel.hasActive
import com.zaneschepke.wireguardautotunnel.core.tunnel.isUp
import com.zaneschepke.wireguardautotunnel.domain.events.AutoTunnelEvent
import com.zaneschepke.wireguardautotunnel.domain.events.KillSwitchEvent
import com.zaneschepke.wireguardautotunnel.domain.model.AppSettings
import com.zaneschepke.wireguardautotunnel.domain.model.TunnelConf
import com.zaneschepke.wireguardautotunnel.util.extensions.isMatchingToWildcardList

data class AutoTunnelState(
    val activeTunnels: Map<TunnelConf, TunnelState> = emptyMap(),
    val networkState: NetworkState = NetworkState(),
    val settings: AppSettings = AppSettings(),
    val tunnels: List<TunnelConf> = emptyList(),
) {

    // also need to check for Wi-Fi state as there is some overlap when they are both connected
    private fun isMobileDataActive(): Boolean {
        return !networkState.isEthernetConnected &&
            !networkState.isWifiConnected &&
            networkState.isMobileDataConnected
    }

    private fun isMobileTunnelDataChangeNeeded(): Boolean {
        val preferredTunnel = preferredMobileDataTunnel()
        return preferredTunnel != null &&
            activeTunnels.isNotEmpty() &&
            !activeTunnels.isUp(preferredTunnel)
    }

    private fun isEthernetTunnelChangeNeeded(): Boolean {
        val preferredTunnel = preferredEthernetTunnel()
        return preferredTunnel != null &&
            activeTunnels.isNotEmpty() &&
            !activeTunnels.isUp(preferredTunnel)
    }

    private fun preferredMobileDataTunnel(): TunnelConf? {
        return tunnels.firstOrNull { it.isMobileDataTunnel }
            ?: tunnels.firstOrNull { it.isPrimaryTunnel }
    }

    private fun preferredEthernetTunnel(): TunnelConf? {
        return tunnels.firstOrNull { it.isEthernetTunnel }
            ?: tunnels.firstOrNull { it.isPrimaryTunnel }
    }

    private fun preferredWifiTunnel(): TunnelConf? {
        return getTunnelWithMatchingTunnelNetwork() ?: tunnels.firstOrNull { it.isPrimaryTunnel }
    }

    // ignore cellular state as there is overlap where it may still be active, but not prioritized
    private fun isWifiActive(): Boolean {
        return !networkState.isEthernetConnected && networkState.isWifiConnected
    }

    private fun startOnEthernet(): Boolean {
        return networkState.isEthernetConnected &&
            settings.isTunnelOnEthernetEnabled &&
            activeTunnels.allDown()
    }

    private fun stopOnEthernet(): Boolean {
        return networkState.isEthernetConnected &&
            !settings.isTunnelOnEthernetEnabled &&
            activeTunnels.hasActive()
    }

    // TODO test removed kill switch state check
    private fun stopKillSwitchOnTrusted(): Boolean {
        return networkState.isWifiConnected &&
            settings.isVpnKillSwitchEnabled &&
            settings.isDisableKillSwitchOnTrustedEnabled &&
            isCurrentSSIDTrusted()
    }

    // TODO test, removed kill switch state check
    private fun startKillSwitch(): Boolean {
        return settings.isVpnKillSwitchEnabled &&
            (!settings.isDisableKillSwitchOnTrustedEnabled || !isCurrentSSIDTrusted())
    }

    private fun isNoConnectivity(): Boolean {
        return !networkState.isEthernetConnected &&
            !networkState.isWifiConnected &&
            !networkState.isMobileDataConnected
    }

    private fun stopOnMobileData(): Boolean {
        return isMobileDataActive() &&
            !settings.isTunnelOnMobileDataEnabled &&
            activeTunnels.hasActive()
    }

    private fun startOnMobileData(): Boolean {
        return isMobileDataActive() &&
            settings.isTunnelOnMobileDataEnabled &&
            activeTunnels.allDown()
    }

    private fun changeOnMobileData(): Boolean {
        return isMobileDataActive() &&
            settings.isTunnelOnMobileDataEnabled &&
            isMobileTunnelDataChangeNeeded()
    }

    private fun changeOnEthernet(): Boolean {
        return networkState.isEthernetConnected &&
            settings.isTunnelOnEthernetEnabled &&
            isEthernetTunnelChangeNeeded()
    }

    private fun stopOnWifi(): Boolean {
        return isWifiActive() && !settings.isTunnelOnWifiEnabled && activeTunnels.hasActive()
    }

    private fun stopOnTrustedWifi(): Boolean {
        return isWifiActive() &&
            settings.isTunnelOnWifiEnabled &&
            activeTunnels.hasActive() &&
            isCurrentSSIDTrusted()
    }

    private fun startOnUntrustedWifi(): Boolean {
        return isWifiActive() &&
            settings.isTunnelOnWifiEnabled &&
            activeTunnels.allDown() &&
            !isCurrentSSIDTrusted()
    }

    private fun changeOnUntrustedWifi(): Boolean {
        return isWifiActive() &&
            settings.isTunnelOnWifiEnabled &&
            activeTunnels.hasActive() &&
            !isCurrentSSIDTrusted() &&
            !isWifiTunnelPreferred()
    }

    private fun isWifiTunnelPreferred(): Boolean {
        val preferred = preferredWifiTunnel()
        return preferred?.let { activeTunnels.isUp(it) } ?: true
    }

    fun asAutoTunnelEvent(): AutoTunnelEvent {
        return when {
            // ethernet scenarios
            stopOnEthernet() -> AutoTunnelEvent.Stop
            startOnEthernet() || changeOnEthernet() ->
                AutoTunnelEvent.Start(preferredEthernetTunnel())
            // mobile data scenarios
            stopOnMobileData() -> AutoTunnelEvent.Stop
            startOnMobileData() || changeOnMobileData() ->
                AutoTunnelEvent.Start(preferredMobileDataTunnel())
            // wifi scenarios
            stopOnWifi() -> AutoTunnelEvent.Stop
            stopOnTrustedWifi() -> AutoTunnelEvent.Stop
            startOnUntrustedWifi() || changeOnUntrustedWifi() ->
                AutoTunnelEvent.Start(preferredWifiTunnel())
            // no connectivity
            isNoConnectivity() && settings.isStopOnNoInternetEnabled -> AutoTunnelEvent.Stop
            else -> AutoTunnelEvent.DoNothing
        }
    }

    fun asKillSwitchEvent(): KillSwitchEvent {
        return when {
            stopKillSwitchOnTrusted() -> KillSwitchEvent.Stop
            startKillSwitch() -> {
                val allowedIps =
                    if (settings.isLanOnKillSwitchEnabled) TunnelConf.LAN_BYPASS_ALLOWED_IPS
                    else emptyList()
                KillSwitchEvent.Start(allowedIps)
            }
            else -> KillSwitchEvent.DoNothing
        }
    }

    private fun isCurrentSSIDTrusted(): Boolean {
        return networkState.wifiName?.let { hasTrustedWifiName(it) } == true
    }

    private fun hasTrustedWifiName(
        wifiName: String,
        wifiNames: List<String> = settings.trustedNetworkSSIDs,
    ): Boolean {
        return if (settings.isWildcardsEnabled) {
            wifiNames.isMatchingToWildcardList(wifiName)
        } else {
            wifiNames.contains(wifiName)
        }
    }

    private fun getTunnelWithMatchingTunnelNetwork(): TunnelConf? {
        return networkState.wifiName?.let { wifiName ->
            tunnels.firstOrNull { hasTrustedWifiName(wifiName, it.tunnelNetworks) }
        }
    }
}
