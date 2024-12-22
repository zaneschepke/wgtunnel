package com.zaneschepke.wireguardautotunnel.service.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.ConnectivityManager.NetworkCallback.FLAG_INCLUDE_LOCATION_INFO
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.SupplicantState
import android.net.wifi.WifiManager
import android.os.Build
import com.wireguard.android.util.RootShell
import com.zaneschepke.wireguardautotunnel.data.repository.SettingsRepository
import com.zaneschepke.wireguardautotunnel.module.AppShell
import com.zaneschepke.wireguardautotunnel.util.extensions.getCurrentWifiName
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Provider

class WifiService
@Inject
constructor(
	@ApplicationContext private val context: Context,
	private val settingsRepository: SettingsRepository,
	@AppShell private val rootShell: Provider<RootShell>
) : NetworkService {

	val mutex = Mutex()

	private var ssid : String? = null
	private var available : Boolean = false

	private val connectivityManager =
		context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

	override val status = callbackFlow {
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
				.addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
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
	}.transform {
		when(it) {
			is NetworkStatus.Available -> mutex.withLock {
				available = true
			}
			is NetworkStatus.CapabilitiesChanged -> mutex.withLock {
				if(available) {
					available = false
					Timber.d("Getting SSID from capabilities")
					ssid = getNetworkName(it.networkCapabilities)
				}
				emit(Status(true, ssid))
			}
			is NetworkStatus.Unavailable -> emit(Status(false, null))
		}
	}

	private suspend fun getNetworkName(networkCapabilities: NetworkCapabilities): String? {
		if(settingsRepository.getSettings().isWifiNameByShellEnabled) return rootShell.get().getCurrentWifiName()
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
