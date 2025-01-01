package com.zaneschepke.wireguardautotunnel.service.network

import android.net.NetworkCapabilities

data class Status(
	val available: Boolean,
	val name: String?,
	val capabilities: NetworkCapabilities?,
)
