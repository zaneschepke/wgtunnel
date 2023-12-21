package com.zaneschepke.wireguardautotunnel.service.network

import android.content.Context
import android.net.NetworkCapabilities
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class MobileDataService
@Inject
constructor(
    @ApplicationContext context: Context
) :
    BaseNetworkService<MobileDataService>(context, NetworkCapabilities.TRANSPORT_CELLULAR)
