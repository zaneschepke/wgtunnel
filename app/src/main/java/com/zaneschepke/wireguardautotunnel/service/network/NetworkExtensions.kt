package com.zaneschepke.wireguardautotunnel.service.network

import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.os.Build

fun NetworkCapabilities.getWifiName(): String? {
	if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
		val info: WifiInfo
		if (transportInfo is WifiInfo) {
			info = transportInfo as WifiInfo
			return info.ssid
		}
	}
	return null
}
