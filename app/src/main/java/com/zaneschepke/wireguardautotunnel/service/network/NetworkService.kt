package com.zaneschepke.wireguardautotunnel.service.network

import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface NetworkService {
	val status: Flow<Status>
	var capabilities: NetworkCapabilities?
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
