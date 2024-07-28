package com.zaneschepke.wireguardautotunnel.service.network

import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.Flow

interface NetworkService<T> {
	fun getNetworkName(networkCapabilities: NetworkCapabilities): String?

	val networkStatus: Flow<NetworkStatus>
}
