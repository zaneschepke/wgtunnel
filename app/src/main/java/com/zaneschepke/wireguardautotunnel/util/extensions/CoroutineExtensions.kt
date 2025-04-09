package com.zaneschepke.wireguardautotunnel.util.extensions

import com.zaneschepke.wireguardautotunnel.ui.state.AppUiState
import java.time.Duration
import java.util.concurrent.ConcurrentLinkedQueue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.channels.ticker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.selects.whileSelect
import timber.log.Timber

/**
 * Chunks based on a time or size threshold.
 *
 * Borrowed from this
 * [Stack Overflow question](https://stackoverflow.com/questions/51022533/kotlin-chunk-sequence-based-on-size-and-time).
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
                        false // done with chunk when timer ticks, takes priority over received
                        // elements
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

fun <K, V> Flow<Map<K, V>>.distinctByKeys(): Flow<Map<K, V>> {
    return distinctUntilChanged { old, new -> old.keys == new.keys }
}

@ExperimentalCoroutinesApi
fun <T> CoroutineScope.asChannel(flow: Flow<T>): ReceiveChannel<T> = produce {
    flow.collect { value -> channel.send(value) }
}

suspend fun <R> StateFlow<AppUiState>.withFirstState(block: suspend (AppUiState) -> R): R {
    return block(first { it.isAppLoaded })
}
