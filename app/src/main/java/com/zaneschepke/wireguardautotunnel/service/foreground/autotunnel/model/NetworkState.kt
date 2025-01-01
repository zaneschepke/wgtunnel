package com.zaneschepke.wireguardautotunnel.service.foreground.autotunnel.model

import android.net.NetworkCapabilities

data class NetworkState(
	val isWifiConnected: Boolean = false,
	val isMobileDataConnected: Boolean = false,
	val isEthernetConnected: Boolean = false,
	val wifiName: String? = null,
	val capabilities: NetworkCapabilities? = null,
)
