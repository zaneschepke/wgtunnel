package com.zaneschepke.networkmonitor

import kotlinx.coroutines.flow.Flow

interface NetworkMonitor {
    val networkStatusFlow: Flow<NetworkStatus>

    fun sendLocationPermissionsGrantedBroadcast()
}
