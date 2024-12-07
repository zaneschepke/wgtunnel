package com.zaneschepke.wireguardautotunnel.service.network

import android.net.Network
import android.net.NetworkCapabilities

sealed class NetworkStatus {
	abstract val isConnected: Boolean
	class Available(val network: Network, override val isConnected: Boolean = true) : NetworkStatus()

	class Unavailable(override val isConnected: Boolean = false) : NetworkStatus()

	class CapabilitiesChanged(val network: Network, val networkCapabilities: NetworkCapabilities, override val isConnected: Boolean = true) :
		NetworkStatus()
}
