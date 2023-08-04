package com.zaneschepke.wireguardautotunnel.service.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.SupplicantState
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map


abstract class BaseNetworkService<T : BaseNetworkService<T>>(val context: Context, networkCapability : Int) : NetworkService<T> {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val wifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    override val networkStatus = callbackFlow {
        val networkStatusCallback = when (Build.VERSION.SDK_INT) {
            in Build.VERSION_CODES.S..Int.MAX_VALUE -> {
                object : ConnectivityManager.NetworkCallback(
                    FLAG_INCLUDE_LOCATION_INFO
                ) {

                    override fun onAvailable(network: Network) {
                        trySend(NetworkStatus.Available(network))
                    }

                    override fun onLost(network: Network) {
                        trySend(NetworkStatus.Unavailable(network))
                    }

                    override fun onCapabilitiesChanged(
                        network: Network,
                        networkCapabilities: NetworkCapabilities
                    ) {
                        trySend(NetworkStatus.CapabilitiesChanged(network, networkCapabilities))
                    }
                }
            }

            else -> {
                object : ConnectivityManager.NetworkCallback() {

                    override fun onAvailable(network: Network) {
                        trySend(NetworkStatus.Available(network))
                    }

                    override fun onLost(network: Network) {
                        trySend(NetworkStatus.Unavailable(network))
                    }

                    override fun onCapabilitiesChanged(
                        network: Network,
                        networkCapabilities: NetworkCapabilities
                    ) {
                        trySend(NetworkStatus.CapabilitiesChanged(network, networkCapabilities))
                    }
                }
            }
        }
        val request = NetworkRequest.Builder()
            .addTransportType(networkCapability)
            .build()
        connectivityManager.registerNetworkCallback(request, networkStatusCallback)

        awaitClose {
            connectivityManager.unregisterNetworkCallback(networkStatusCallback)
        }
    }


    override fun getNetworkName(networkCapabilities: NetworkCapabilities): String? {
        var ssid: String? = getWifiNameFromCapabilities(networkCapabilities)
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
            val info = wifiManager.connectionInfo
            if (info.supplicantState === SupplicantState.COMPLETED) {
                ssid = info.ssid
            }
        }
        return ssid?.trim('"')
    }


    companion object {
        private fun getWifiNameFromCapabilities(networkCapabilities: NetworkCapabilities): String? {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val info: WifiInfo
                if (networkCapabilities.transportInfo is WifiInfo) {
                    info = networkCapabilities.transportInfo as WifiInfo
                    return info.ssid
                }
            }
            return null
        }
    }
}

inline fun <Result> Flow<NetworkStatus>.map(
    crossinline onUnavailable: suspend (network : Network) -> Result,
    crossinline onAvailable: suspend (network : Network) -> Result,
    crossinline onCapabilitiesChanged: suspend (network : Network, networkCapabilities : NetworkCapabilities) -> Result,
): Flow<Result> = map { status ->
    when (status) {
        is NetworkStatus.Unavailable -> onUnavailable(status.network)
        is NetworkStatus.Available -> onAvailable(status.network)
        is NetworkStatus.CapabilitiesChanged -> onCapabilitiesChanged(status.network, status.networkCapabilities)
    }
}