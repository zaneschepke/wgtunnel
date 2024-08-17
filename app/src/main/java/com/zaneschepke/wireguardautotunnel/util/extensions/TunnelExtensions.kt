package com.zaneschepke.wireguardautotunnel.util.extensions

import com.zaneschepke.wireguardautotunnel.service.tunnel.HandshakeStatus
import com.zaneschepke.wireguardautotunnel.service.tunnel.statistics.TunnelStatistics
import com.zaneschepke.wireguardautotunnel.util.Constants
import com.zaneschepke.wireguardautotunnel.util.NumberUtils
import org.amnezia.awg.config.Config

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
	val amQuick = toAwgQuickString(true)
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
