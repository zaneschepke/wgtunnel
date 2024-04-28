package com.zaneschepke.wireguardautotunnel.util

import android.content.BroadcastReceiver
import android.content.pm.PackageInfo
import com.zaneschepke.wireguardautotunnel.data.model.TunnelConfig
import com.zaneschepke.wireguardautotunnel.service.tunnel.HandshakeStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.amnezia.awg.backend.Statistics
import org.amnezia.awg.crypto.Key
import java.math.BigDecimal
import java.text.DecimalFormat
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

fun BroadcastReceiver.goAsync(
    context: CoroutineContext = EmptyCoroutineContext,
    block: suspend CoroutineScope.() -> Unit
) {
    val pendingResult = goAsync()
    @OptIn(DelicateCoroutinesApi::class) // Must run globally; there's no teardown callback.
    GlobalScope.launch(context) {
        try {
            block()
        } finally {
            pendingResult.finish()
        }
    }
}

fun String.truncateWithEllipsis(allowedLength: Int): String {
    return if (this.length > allowedLength + 3) {
        this.substring(0, allowedLength) + "***"
    } else this
}

fun BigDecimal.toThreeDecimalPlaceString(): String {
    val df = DecimalFormat("#.###")
    return df.format(this)
}

fun <T> List<T>.update(index: Int, item: T): List<T> = toMutableList().apply { this[index] = item }

fun <T> List<T>.removeAt(index: Int): List<T> = toMutableList().apply { this.removeAt(index) }

typealias TunnelConfigs = List<TunnelConfig>

typealias Packages = List<PackageInfo>

fun Statistics.mapPeerStats(): Map<Key, Statistics.PeerStats?> {
    return this.peers().associateWith { key -> (this.peer(key)) }
}

fun Statistics.PeerStats.latestHandshakeSeconds(): Long? {
    return NumberUtils.getSecondsBetweenTimestampAndNow(this.latestHandshakeEpochMillis)
}

fun Statistics.PeerStats.handshakeStatus(): HandshakeStatus {
    // TODO add never connected status after duration
    return this.latestHandshakeSeconds().let {
        when {
            it == null -> HandshakeStatus.NOT_STARTED
            it <= HandshakeStatus.STALE_TIME_LIMIT_SEC -> HandshakeStatus.HEALTHY
            it > HandshakeStatus.STALE_TIME_LIMIT_SEC -> HandshakeStatus.STALE
            else -> {
                HandshakeStatus.UNKNOWN
            }
        }
    }
}
