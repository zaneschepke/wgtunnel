package com.zaneschepke.networkmonitor

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import com.wireguard.android.util.RootShell
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import timber.log.Timber

class AndroidNetworkMonitor(
	context: Context,
) : NetworkMonitor {

	private val appContext = context.applicationContext
	private val connectivityManager = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
	private val wifiManager = appContext.getSystemService(Context.WIFI_SERVICE) as WifiManager?
	private val rootShell = RootShell(context)

	private var includeWifiSsid = false
	private var useRootShell = false

	data class WifiState(val connected: Boolean = false, val ssid: String? = null)
	data class TransportState(val connected: Boolean = false)

	private val wifiFlow: Flow<WifiState> = callbackFlow {
		var currentSsid: String? = null

		@Suppress("DEPRECATION")
		fun getWifiSsid(): String? {
			if (!includeWifiSsid || wifiManager == null) return null
			return if (useRootShell) {
				rootShell.getCurrentWifiName()
			} else {
				wifiManager.connectionInfo?.ssid?.trim('"')?.takeIf {
					it != "<unknown>" && it.isNotEmpty()
				}
			}
		}

		val callback = object : ConnectivityManager.NetworkCallback() {
			override fun onAvailable(network: Network) {
				Timber.d("Wi-Fi onAvailable: network=$network")
				val capabilities = connectivityManager.getNetworkCapabilities(network)
				val connected = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
				if (connected) {
					val ssid = getWifiSsid()
					currentSsid = ssid
					trySend(WifiState(connected = true, ssid = ssid))
				}
			}

			override fun onLost(network: Network) {
				Timber.d("Wi-Fi onLost: network=$network")
				currentSsid = null
				trySend(WifiState(connected = false, ssid = null))
			}

			override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
				val connected = networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
				val ssid = if (connected) getWifiSsid() else null
				if (ssid != currentSsid) {
					currentSsid = ssid
					trySend(WifiState(connected = connected, ssid = ssid))
				}
			}
		}

		val request = NetworkRequest.Builder()
			.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
			.addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
			.build()

		connectivityManager.registerNetworkCallback(request, callback)
		trySend(WifiState())

		awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
	}

	private val cellularFlow: Flow<TransportState> = callbackFlow {
		val callback = object : ConnectivityManager.NetworkCallback() {
			override fun onAvailable(network: Network) {
				Timber.d("Cellular onAvailable: network=$network")
				trySend(TransportState(connected = true))
			}

			override fun onLost(network: Network) {
				Timber.d("Cellular onLost: network=$network")
				trySend(TransportState(connected = false))
			}
		}

		val request = NetworkRequest.Builder()
			.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
			.addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
			.build()

		connectivityManager.registerNetworkCallback(request, callback)
		trySend(TransportState())

		awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
	}

	private val ethernetFlow: Flow<TransportState> = callbackFlow {
		val callback = object : ConnectivityManager.NetworkCallback() {
			override fun onAvailable(network: Network) {
				Timber.d("Ethernet onAvailable: network=$network")
				trySend(TransportState(connected = true))
			}

			override fun onLost(network: Network) {
				Timber.d("Ethernet onLost: network=$network")
				trySend(TransportState(connected = false))
			}
		}

		val request = NetworkRequest.Builder()
			.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
			.addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
			.build()

		connectivityManager.registerNetworkCallback(request, callback)
		trySend(TransportState())

		awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
	}

	override fun getNetworkStatusFlow(includeWifiSsid: Boolean, useRootShell: Boolean): Flow<NetworkStatus> {
		this.includeWifiSsid = includeWifiSsid
		this.useRootShell = useRootShell
		return combine(wifiFlow, cellularFlow, ethernetFlow) { wifi, cellular, ethernet ->
			val hasAnyConnection = wifi.connected || cellular.connected || ethernet.connected
			if (hasAnyConnection) {
				NetworkStatus.Connected(
					wifiSsid = wifi.ssid,
					wifiConnected = wifi.connected,
					cellularConnected = cellular.connected,
					ethernetConnected = ethernet.connected,
				)
			} else {
				NetworkStatus.Disconnected
			}.also { Timber.d("NetworkStatus: $it") }
		}.distinctUntilChanged()
	}
}
