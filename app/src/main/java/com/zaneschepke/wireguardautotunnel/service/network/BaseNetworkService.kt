package com.zaneschepke.wireguardautotunnel.service.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.os.Build
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.map
import timber.log.Timber

abstract class BaseNetworkService<T : BaseNetworkService<T>>(
	val context: Context,
	networkCapability: Int,
) : NetworkService<T> {
	private val connectivityManager =
		context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

	val wifiManager =
		context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

	fun checkHasCapability(networkCapability: Int): Boolean {
		val network = connectivityManager.activeNetwork
		val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
		return networkCapabilities?.hasTransport(networkCapability) == true
	}

	override val networkStatus =
		callbackFlow {
			if (!checkHasCapability(networkCapability)) {
				trySend(NetworkStatus.Unavailable())
			}
			val networkStatusCallback =
				when (Build.VERSION.SDK_INT) {
					in Build.VERSION_CODES.S..Int.MAX_VALUE -> {
						object :
							ConnectivityManager.NetworkCallback(
								FLAG_INCLUDE_LOCATION_INFO,
							) {
							override fun onAvailable(network: Network) {
								trySend(NetworkStatus.Available(network))
							}

							override fun onLost(network: Network) {
								trySend(NetworkStatus.Unavailable())
							}

							override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
								trySend(
									NetworkStatus.CapabilitiesChanged(
										network,
										networkCapabilities,
									),
								)
							}
						}
					}

					else -> {
						object : ConnectivityManager.NetworkCallback() {
							override fun onAvailable(network: Network) {
								trySend(NetworkStatus.Available(network))
							}

							override fun onLost(network: Network) {
								trySend(NetworkStatus.Unavailable())
							}

							override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
								trySend(
									NetworkStatus.CapabilitiesChanged(
										network,
										networkCapabilities,
									),
								)
							}
						}
					}
				}
			val request =
				NetworkRequest.Builder()
					.addTransportType(networkCapability)
					.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
					.addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
					.build()
			connectivityManager.registerNetworkCallback(request, networkStatusCallback)

			awaitClose { connectivityManager.unregisterNetworkCallback(networkStatusCallback) }
		}.catch {
			Timber.e(it)
			// conflate for backpressure
		}.conflate()
}

inline fun <Result> Flow<NetworkStatus>.map(
	crossinline onUnavailable: suspend () -> Result,
	crossinline onAvailable: suspend (network: Network) -> Result,
	crossinline onCapabilitiesChanged:
	suspend (network: Network, networkCapabilities: NetworkCapabilities) -> Result,
): Flow<Result> = map { status ->
	when (status) {
		is NetworkStatus.Unavailable -> onUnavailable()
		is NetworkStatus.Available -> onAvailable(status.network)
		is NetworkStatus.CapabilitiesChanged ->
			onCapabilitiesChanged(
				status.network,
				status.networkCapabilities,
			)
	}
}
