package com.zaneschepke.wireguardautotunnel.domain.entity

data class AppSettings(
    val id: Int = 0,
    val isAutoTunnelEnabled: Boolean = false,
    val isTunnelOnMobileDataEnabled: Boolean = false,
    val trustedNetworkSSIDs: List<String> = emptyList(),
    val isAlwaysOnVpnEnabled: Boolean = false,
    val isTunnelOnEthernetEnabled: Boolean = false,
    val isShortcutsEnabled: Boolean = false,
    val isTunnelOnWifiEnabled: Boolean = false,
    val isKernelEnabled: Boolean = false,
    val isRestoreOnBootEnabled: Boolean = false,
    val isMultiTunnelEnabled: Boolean = false,
    val isPingEnabled: Boolean = false,
    val isAmneziaEnabled: Boolean = false,
    val isWildcardsEnabled: Boolean = false,
    val isWifiNameByShellEnabled: Boolean = false,
    val isStopOnNoInternetEnabled: Boolean = false,
    val isVpnKillSwitchEnabled: Boolean = false,
    val isKernelKillSwitchEnabled: Boolean = false,
    val isLanOnKillSwitchEnabled: Boolean = false,
    val debounceDelaySeconds: Int = 3,
    val isDisableKillSwitchOnTrustedEnabled: Boolean = false,
) {
    fun debounceDelayMillis(): Long {
        return debounceDelaySeconds * 1000L
    }

    fun toAutoTunnelStateString(): String {
        return """
            TunnelOnWifi: $isTunnelOnWifiEnabled
            TunnelOnMobileData: $isTunnelOnMobileDataEnabled
            TunnelOnEthernet: $isTunnelOnEthernetEnabled
            Wildcards: $isWildcardsEnabled
            StopOnNoInternet: $isStopOnNoInternetEnabled
            Trusted Networks: $trustedNetworkSSIDs
        """.trimIndent()
    }
}
