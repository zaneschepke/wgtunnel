package com.zaneschepke.wireguardautotunnel.service.network

import android.content.Context
import android.net.NetworkCapabilities
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class EthernetService @Inject constructor(@ApplicationContext context: Context) :
    BaseNetworkService<EthernetService>(context, NetworkCapabilities.TRANSPORT_ETHERNET) {
}