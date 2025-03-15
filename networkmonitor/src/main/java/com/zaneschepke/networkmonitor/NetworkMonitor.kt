package com.zaneschepke.networkmonitor

import kotlinx.coroutines.flow.Flow

interface NetworkMonitor {
	fun getNetworkStatusFlow(includeWifiSsid: Boolean, useRootShell: Boolean): Flow<NetworkStatus>
}
