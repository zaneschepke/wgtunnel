package com.zaneschepke.networkmonitor

sealed class NetworkStatus {
    data object Disconnected : NetworkStatus() {
        override val wifiConnected = false
        override val ethernetConnected = false
        override val cellularConnected = false
    }

    data class Connected(
        val wifiSsid: String? = null,
        val securityType: WifiSecurityType? = null,
        override val wifiConnected: Boolean = false,
        override val ethernetConnected: Boolean = false,
        override val cellularConnected: Boolean = false,
    ) : NetworkStatus()

    abstract val wifiConnected: Boolean
    abstract val ethernetConnected: Boolean
    abstract val cellularConnected: Boolean
}
