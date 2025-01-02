package com.zaneschepke.wireguardautotunnel.service.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import timber.log.Timber
import javax.inject.Inject

class EthernetService
@Inject
constructor(
	@ApplicationContext context: Context,
) : NetworkService {

	override var capabilities: NetworkCapabilities? = null

	private val connectivityManager =
		context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

	override val status = callbackFlow {
		val networkStatusCallback = object : ConnectivityManager.NetworkCallback() {
			override fun onAvailable(network: Network) {
				trySend(NetworkStatus.Available(network))
			}
			override fun onLost(network: Network) {
				trySend(NetworkStatus.Unavailable())
			}
			override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
				capabilities = networkCapabilities
				trySend(
					NetworkStatus.CapabilitiesChanged(
						network,
						networkCapabilities,
					),
				)
			}
		}
		val request =
			NetworkRequest.Builder()
				.addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
				.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
				.addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
				.build()
		connectivityManager.registerNetworkCallback(request, networkStatusCallback)

		awaitClose { connectivityManager.unregisterNetworkCallback(networkStatusCallback) }
	}.onStart {
		// needed for services that are not yet available as it will impact later combine flows if we don't emit
		emit(NetworkStatus.Unavailable())
	}.catch {
		Timber.e(it)
		emit(NetworkStatus.Unavailable())
	}.map {
		when (it) {
			is NetworkStatus.Available, is NetworkStatus.CapabilitiesChanged -> Status(true, null)
			is NetworkStatus.Unavailable -> Status(false, null)
		}
	}
}
