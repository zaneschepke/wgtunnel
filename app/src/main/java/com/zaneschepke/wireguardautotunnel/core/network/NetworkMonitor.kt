package com.zaneschepke.wireguardautotunnel.core.network

import android.net.NetworkCapabilities
import com.zaneschepke.wireguardautotunnel.domain.state.ConnectivityState
import kotlinx.coroutines.flow.Flow

interface NetworkMonitor {
	val status: Flow<ConnectivityState>

	// util to help limit location queries
	val didWifiChangeSinceLastCapabilitiesQuery: Boolean
	fun getWifiCapabilities(): NetworkCapabilities?
}
