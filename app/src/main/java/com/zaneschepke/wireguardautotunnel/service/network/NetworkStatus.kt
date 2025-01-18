package com.zaneschepke.wireguardautotunnel.service.network

data class NetworkStatus(
	val wifiAvailable: Boolean,
	val ethernetAvailable: Boolean,
	val cellularAvailable: Boolean,
) {
	val allOffline = !wifiAvailable && !ethernetAvailable && !cellularAvailable
}
