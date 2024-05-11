package com.zaneschepke.wireguardautotunnel.service.foreground

import com.zaneschepke.wireguardautotunnel.data.domain.Settings
import com.zaneschepke.wireguardautotunnel.data.domain.TunnelConfig

data class WatcherState(
    val isWifiConnected: Boolean = false,
    val config: TunnelConfig? = null,
    //val vpnStatus: TunnelState = TunnelState.DOWN,
    val isEthernetConnected: Boolean = false,
    val isMobileDataConnected: Boolean = false,
    val currentNetworkSSID: String = "",
    val settings: Settings = Settings()
) {

    //private fun isVpnConnected() = vpnStatus == TunnelState.UP
    fun isEthernetConditionMet(): Boolean {
        return (isEthernetConnected &&
            settings.isTunnelOnEthernetEnabled)
    }

    fun isMobileDataConditionMet(): Boolean {
        return (!isEthernetConnected &&
            settings.isTunnelOnMobileDataEnabled &&
            !isWifiConnected &&
            isMobileDataConnected)
    }

    fun isTunnelOnMobileDataPreferredConditionMet(): Boolean {
        return (!isEthernetConnected &&
            settings.isTunnelOnMobileDataEnabled &&
            !isWifiConnected &&
            isMobileDataConnected &&
            config?.isMobileDataTunnel == false)
    }

    fun isTunnelOffOnMobileDataConditionMet(): Boolean {
        return (!isEthernetConnected &&
            !settings.isTunnelOnMobileDataEnabled &&
            isMobileDataConnected &&
            !isWifiConnected)
    }

    fun isUntrustedWifiConditionMet(): Boolean {
        return (!isEthernetConnected &&
            isWifiConnected &&
            !settings.trustedNetworkSSIDs.contains(currentNetworkSSID) &&
            settings.isTunnelOnWifiEnabled)
    }

    fun isTunnelNotWifiNamePreferredMet(ssid: String): Boolean {
        return (!isEthernetConnected &&
            isWifiConnected &&
            !settings.trustedNetworkSSIDs.contains(currentNetworkSSID) &&
            settings.isTunnelOnWifiEnabled && config?.tunnelNetworks?.contains(ssid) == false)
    }

    fun isTrustedWifiConditionMet(): Boolean {
        return (!isEthernetConnected &&
            (isWifiConnected &&
                settings.trustedNetworkSSIDs.contains(currentNetworkSSID)))
    }

    fun isTunnelOffOnWifiConditionMet(): Boolean {
        return (!isEthernetConnected &&
            (isWifiConnected &&
                !settings.isTunnelOnWifiEnabled))
    }

    fun isTunnelOffOnNoConnectivityMet(): Boolean {
        return (!isEthernetConnected &&
            !isWifiConnected &&
            !isMobileDataConnected)
    }
}

