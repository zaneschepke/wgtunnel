package com.zaneschepke.wireguardautotunnel.util

import android.content.Context
import android.content.pm.PackageInfo
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.data.domain.TunnelConfig
import com.zaneschepke.wireguardautotunnel.service.tunnel.HandshakeStatus
import com.zaneschepke.wireguardautotunnel.service.tunnel.statistics.TunnelStatistics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.channels.ticker
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.selects.whileSelect
import org.amnezia.awg.config.Config
import timber.log.Timber
import java.math.BigDecimal
import java.text.DecimalFormat
import java.time.Duration
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.coroutines.cancellation.CancellationException

fun BigDecimal.toThreeDecimalPlaceString(): String {
    val df = DecimalFormat("#.###")
    return df.format(this)
}

fun <T> List<T>.update(index: Int, item: T): List<T> = toMutableList().apply { this[index] = item }

fun <T> List<T>.removeAt(index: Int): List<T> = toMutableList().apply { this.removeAt(index) }

typealias TunnelConfigs = List<TunnelConfig>

typealias Packages = List<PackageInfo>

fun TunnelStatistics.mapPeerStats(): Map<org.amnezia.awg.crypto.Key, TunnelStatistics.PeerStats?> {
    return this.getPeers().associateWith { key -> (this.peerStats(key)) }
}

fun TunnelStatistics.PeerStats.latestHandshakeSeconds(): Long? {
    return NumberUtils.getSecondsBetweenTimestampAndNow(this.latestHandshakeEpochMillis)
}

fun TunnelStatistics.PeerStats.handshakeStatus(): HandshakeStatus {
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

fun Config.toWgQuickString(): String {
    val amQuick = toAwgQuickString()
    val lines = amQuick.lines().toMutableList()
    val linesIterator = lines.iterator()
    while (linesIterator.hasNext()) {
        val next = linesIterator.next()
        Constants.amneziaProperties.forEach {
            if (next.startsWith(it, ignoreCase = true)) {
                linesIterator.remove()
            }
        }
    }
    return lines.joinToString(System.lineSeparator())
}

fun Throwable.getMessage(context: Context): String {
    return when (this) {
        is WgTunnelExceptions -> this.getMessage(context)
        else -> this.message ?: StringValue.StringResource(R.string.unknown_error).asString(context)
    }
}

/**
 * Chunks based on a time or size threshold.
 *
 * Borrowed from this [Stack Overflow question](https://stackoverflow.com/questions/51022533/kotlin-chunk-sequence-based-on-size-and-time).
 */
@OptIn(ObsoleteCoroutinesApi::class, ExperimentalCoroutinesApi::class)
fun <T> ReceiveChannel<T>.chunked(scope: CoroutineScope, size: Int, time: Duration) =
    scope.produce<List<T>> {
        while (true) { // this loop goes over each chunk
            val chunk = ConcurrentLinkedQueue<T>() // current chunk
            val ticker = ticker(time.toMillis()) // time-limit for this chunk
            try {
                whileSelect {
                    ticker.onReceive {
                        false // done with chunk when timer ticks, takes priority over received elements
                    }
                    this@chunked.onReceive {
                        chunk += it
                        chunk.size < size // continue whileSelect if chunk is not full
                    }
                }
            } catch (e: ClosedReceiveChannelException) {
                Timber.e(e)
                return@produce
            } finally {
                ticker.cancel()
                if (chunk.isNotEmpty()) {
                    send(chunk.toList())
                }
            }
        }
    }

@OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class)
fun <T> Flow<T>.chunked(size: Int, time: Duration) = channelFlow {
    coroutineScope {
        val channel = asChannel(this@chunked).chunked(this, size, time)
        try {
            while (!channel.isClosedForReceive) {
                send(channel.receive())
            }
        } catch (e: ClosedReceiveChannelException) {
            // Channel was closed by the flow completing, nothing to do
            Timber.w(e)
        } catch (e: CancellationException) {
            channel.cancel(e)
            throw e
        } catch (e: Exception) {
            channel.cancel(CancellationException("Closing channel due to flow exception", e))
            throw e
        }
    }
}

@ExperimentalCoroutinesApi
fun <T> CoroutineScope.asChannel(flow: Flow<T>): ReceiveChannel<T> = produce {
    flow.collect { value ->
        channel.send(value)
    }
}



