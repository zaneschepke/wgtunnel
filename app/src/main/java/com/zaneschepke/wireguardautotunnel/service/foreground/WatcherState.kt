package com.zaneschepke.wireguardautotunnel.service.foreground

import com.wireguard.android.backend.Tunnel
import com.zaneschepke.wireguardautotunnel.data.model.Settings
import com.zaneschepke.wireguardautotunnel.data.model.TunnelConfig

data class WatcherState(
    val isWifiConnected: Boolean = false,
    val config: TunnelConfig? = null,
    val vpnStatus: Tunnel.State = Tunnel.State.DOWN,
    val isEthernetConnected: Boolean = false,
    val isMobileDataConnected: Boolean = false,
    val currentNetworkSSID: String = "",
    val settings: Settings = Settings()
) {

    private fun isVpnConnected() = vpnStatus == Tunnel.State.UP
    fun isEthernetConditionMet(): Boolean {
        return (isEthernetConnected &&
            settings.isTunnelOnEthernetEnabled &&
            !isVpnConnected())
    }

    fun isMobileDataConditionMet(): Boolean {
        return (!isEthernetConnected &&
            settings.isTunnelOnMobileDataEnabled &&
            !isWifiConnected &&
            isMobileDataConnected &&
            !isVpnConnected())
    }

    fun isTunnelNotMobileDataPreferredConditionMet(): Boolean {
        return (!isEthernetConnected &&
            settings.isTunnelOnMobileDataEnabled &&
            !isWifiConnected &&
            isMobileDataConnected &&
            config?.isMobileDataTunnel == false && isVpnConnected())
    }

    fun isTunnelOffOnMobileDataConditionMet(): Boolean {
        return (!isEthernetConnected &&
            !settings.isTunnelOnMobileDataEnabled &&
            isMobileDataConnected &&
            !isWifiConnected &&
            isVpnConnected())
    }

    fun isUntrustedWifiConditionMet(): Boolean {
        return (!isEthernetConnected &&
            isWifiConnected &&
            !settings.trustedNetworkSSIDs.contains(currentNetworkSSID) &&
            settings.isTunnelOnWifiEnabled
            && !isVpnConnected())
    }

    fun isTunnelNotWifiNamePreferredMet(ssid: String): Boolean {
        return (!isEthernetConnected &&
            isWifiConnected &&
            !settings.trustedNetworkSSIDs.contains(currentNetworkSSID) &&
            settings.isTunnelOnWifiEnabled && config?.tunnelNetworks?.contains(ssid) == false && isVpnConnected())
    }

    fun isTrustedWifiConditionMet(): Boolean {
        return (!isEthernetConnected &&
            (isWifiConnected &&
                settings.trustedNetworkSSIDs.contains(currentNetworkSSID)) &&
            (isVpnConnected()))
    }

    fun isTunnelOffOnWifiConditionMet(): Boolean {
        return (!isEthernetConnected &&
            (isWifiConnected &&
                !settings.isTunnelOnWifiEnabled &&
                (isVpnConnected())))
    }

    fun isTunnelOffOnNoConnectivityMet(): Boolean {
        return (!isEthernetConnected &&
            !isWifiConnected &&
            !isMobileDataConnected &&
            (isVpnConnected()))
    }
}

