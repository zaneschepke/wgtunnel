package com.zaneschepke.wireguardautotunnel.service.network

import android.content.Context
import android.net.NetworkCapabilities
import android.net.wifi.SupplicantState
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class WifiService
@Inject
constructor(
	@ApplicationContext context: Context,
) :
	BaseNetworkService<WifiService>(context, NetworkCapabilities.TRANSPORT_WIFI) {

	override fun getNetworkName(networkCapabilities: NetworkCapabilities): String? {
		var ssid = networkCapabilities.getWifiName()
		if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S) {
			val info = wifiManager.connectionInfo
			if (info.supplicantState === SupplicantState.COMPLETED) {
				ssid = info.ssid
			}
		}
		return ssid?.trim('"')
	}

	override fun isNetworkSecure(): Boolean {
		// TODO
		return false
	}
}
