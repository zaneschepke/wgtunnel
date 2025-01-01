package com.zaneschepke.wireguardautotunnel.util.extensions

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.channels.ticker
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.selects.whileSelect
import timber.log.Timber
import java.time.Duration
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.coroutines.cancellation.CancellationException

/**
 * Chunks based on a time or size threshold.
 *
 * Borrowed from this [Stack Overflow question](https://stackoverflow.com/questions/51022533/kotlin-chunk-sequence-based-on-size-and-time).
 */
@OptIn(ObsoleteCoroutinesApi::class, ExperimentalCoroutinesApi::class)
fun <T> ReceiveChannel<T>.chunked(scope: CoroutineScope, size: Int, time: Duration) = scope.produce<List<T>> {
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

fun Job.cancelWithMessage(message: String) {
	kotlin.runCatching {
		cancel()
		Timber.i(message)
	}
}
