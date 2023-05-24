package com.zaneschepke.wireguardautotunnel.service.network

import android.content.Context
import android.net.NetworkCapabilities
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class WifiService @Inject constructor(@ApplicationContext context: Context) :
    BaseNetworkService<WifiService>(context, NetworkCapabilities.TRANSPORT_WIFI) {
}