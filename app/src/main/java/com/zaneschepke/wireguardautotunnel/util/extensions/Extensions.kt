package com.zaneschepke.wireguardautotunnel.util.extensions

import android.content.Context
import android.content.pm.PackageInfo
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.data.domain.TunnelConfig
import com.zaneschepke.wireguardautotunnel.util.StringValue
import com.zaneschepke.wireguardautotunnel.util.WgTunnelExceptions
import java.math.BigDecimal
import java.text.DecimalFormat

fun BigDecimal.toThreeDecimalPlaceString(): String {
	val df = DecimalFormat("#.###")
	return df.format(this)
}

fun <T> List<T>.update(index: Int, item: T): List<T> = toMutableList().apply { this[index] = item }

fun <T> List<T>.removeAt(index: Int): List<T> = toMutableList().apply { this.removeAt(index) }

typealias TunnelConfigs = List<TunnelConfig>

typealias Packages = List<PackageInfo>

fun Throwable.getMessage(context: Context): String {
	return when (this) {
		is WgTunnelExceptions -> this.getMessage(context)
		else -> this.message ?: StringValue.StringResource(R.string.unknown_error).asString(context)
	}
}
