package com.zaneschepke.wireguardautotunnel.service.network

import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.Flow

interface NetworkService {
	val status: Flow<NetworkStatus>

	// util to help limit location queries
	val didWifiChangeSinceLastCapabilitiesQuery: Boolean
	fun getWifiCapabilities(): NetworkCapabilities?
}
