package com.zaneschepke.wireguardautotunnel.service.network

import android.net.Network
import android.net.NetworkCapabilities

sealed class NetworkStatus {
    class Available(val network : Network) : NetworkStatus()
    class Unavailable(val network : Network) : NetworkStatus()
    class CapabilitiesChanged(val network : Network, val networkCapabilities : NetworkCapabilities) : NetworkStatus()
}
