package com.zaneschepke.wireguardautotunnel.service.foreground.autotunnel.model

data class NetworkState(
	val isWifiConnected: Boolean = false,
	val isMobileDataConnected: Boolean = false,
	val isEthernetConnected: Boolean = false,
	val wifiName: String? = null,
)
