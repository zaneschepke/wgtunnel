package com.zaneschepke.networkmonitor

import android.net.wifi.WifiManager
import android.os.Build
import com.wireguard.android.util.RootShell

fun RootShell.getCurrentWifiName(): String? {
    val response = mutableListOf<String>()
    this.run(
        response,
        "dumpsys wifi | grep 'Supplicant state: COMPLETED' | grep -o 'SSID: [^,]*' | cut -d ' ' -f2- | tr -d '\"'",
    )
    return response.firstOrNull()
}

@Suppress("DEPRECATION")
fun WifiManager.getCurrentSecurityType(): WifiSecurityType? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        WifiSecurityType.from(connectionInfo.currentSecurityType)
    } else {
        null
    }
}
