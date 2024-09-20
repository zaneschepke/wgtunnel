package com.zaneschepke.wireguardautotunnel.util.extensions

import android.content.pm.PackageInfo
import com.zaneschepke.wireguardautotunnel.data.domain.TunnelConfig
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
