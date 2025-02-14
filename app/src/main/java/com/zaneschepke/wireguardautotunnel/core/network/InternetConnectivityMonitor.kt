package com.zaneschepke.wireguardautotunnel.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.SupplicantState
import android.net.wifi.WifiManager
import android.os.Build
import com.zaneschepke.wireguardautotunnel.domain.state.ConnectivityState
import com.zaneschepke.wireguardautotunnel.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class InternetConnectivityMonitor
@Inject
constructor(
	@ApplicationContext private val context: Context,
	@IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : NetworkMonitor {

	private val connectivityManager =
		context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

	@get:Synchronized @set:Synchronized
	private var wifiCapabilities: NetworkCapabilities? = null

	@get:Synchronized @set:Synchronized
	private var wifiNetworkChanged: Boolean = false

	override val didWifiChangeSinceLastCapabilitiesQuery: Boolean
		get() = wifiNetworkChanged

	override val status = callbackFlow {

		var wifiState: Boolean = false
		var ethernetState: Boolean = false
		var cellularState: Boolean = false

		fun emitState() {
			trySend(ConnectivityState(wifiState, ethernetState, cellularState))
		}

		val currentNetwork = connectivityManager.activeNetwork
		if (currentNetwork == null) {
			emitState()
		}

		fun updateCapabilityState(up: Boolean, network: Network) {
			with(connectivityManager.getNetworkCapabilities(network)) {
				when {
					this == null -> return
					hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> wifiState = up
					hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ->
						cellularState = up

					hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ->
						ethernetState = up
				}
			}
		}

		fun onWifiChange(network: Network, callback: () -> Unit) {
			if (connectivityManager.getNetworkCapabilities(network)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true) {
				callback()
			}
		}

		fun onAvailable(network: Network) {
			onWifiChange(network) {
				wifiNetworkChanged = true
			}
		}

		fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
			onWifiChange(network) {
				wifiCapabilities = networkCapabilities
			}
			updateCapabilityState(true, network)
			emitState()
		}

		val networkStatusCallback =
			when (Build.VERSION.SDK_INT) {
				in Build.VERSION_CODES.S..Int.MAX_VALUE -> {
					object :
						ConnectivityManager.NetworkCallback(
							FLAG_INCLUDE_LOCATION_INFO,
						) {
						override fun onAvailable(network: Network) {
							onAvailable(network)
						}

						override fun onLost(network: Network) {
							updateCapabilityState(false, network)
							emitState()
						}

						override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
							onCapabilitiesChanged(network, networkCapabilities)
						}
					}
				}

				else -> {
					object : ConnectivityManager.NetworkCallback() {
						override fun onAvailable(network: Network) {
							onAvailable(network)
						}

						override fun onLost(network: Network) {
							updateCapabilityState(false, network)
							emitState()
						}

						override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
							onCapabilitiesChanged(network, networkCapabilities)
						}
					}
				}
			}
		val request =
			NetworkRequest.Builder()
				.addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
				.addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
				.addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
				.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
				.addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
				.build()
		connectivityManager.registerNetworkCallback(request, networkStatusCallback)

		awaitClose { connectivityManager.unregisterNetworkCallback(networkStatusCallback) }
	}.flowOn(ioDispatcher)

	override fun getWifiCapabilities(): NetworkCapabilities? {
		wifiNetworkChanged = false
		return wifiCapabilities
	}

	companion object {
		fun getNetworkName(networkCapabilities: NetworkCapabilities, context: Context): String? {
			var ssid = networkCapabilities.getWifiName()
			if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S) {
				val wifiManager =
					context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

				@Suppress("DEPRECATION")
				val info = wifiManager.connectionInfo
				if (info.supplicantState === SupplicantState.COMPLETED) {
					ssid = info.ssid
				}
			}
			return ssid?.trim('"')
		}
	}
}
