package com.zaneschepke.networkmonitor

import com.wireguard.android.util.RootShell

fun RootShell.getCurrentWifiName(): String? {
    val response = mutableListOf<String>()
    this.run(
        response,
        "dumpsys wifi | grep 'Supplicant state: COMPLETED' | grep -o 'SSID: [^,]*' | cut -d ' ' -f2- | tr -d '\"'",
    )
    return response.firstOrNull()
}
