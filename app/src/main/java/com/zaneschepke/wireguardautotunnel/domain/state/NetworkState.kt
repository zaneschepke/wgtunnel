package com.zaneschepke.wireguardautotunnel.domain.state

data class NetworkState(
    val isWifiConnected: Boolean = false,
    val isMobileDataConnected: Boolean = false,
    val isEthernetConnected: Boolean = false,
    val wifiName: String? = null,
) {
    fun hasNoCapabilities(): Boolean {
        return !isWifiConnected && !isMobileDataConnected && !isEthernetConnected
    }
}
