package com.zaneschepke.wireguardautotunnel.util.extensions

import androidx.compose.ui.graphics.Color
import com.wireguard.android.util.RootShell
import com.wireguard.config.Peer
import com.zaneschepke.wireguardautotunnel.service.tunnel.BackendState
import com.zaneschepke.wireguardautotunnel.service.tunnel.HandshakeStatus
import com.zaneschepke.wireguardautotunnel.service.tunnel.statistics.TunnelStatistics
import com.zaneschepke.wireguardautotunnel.ui.screens.tunneloptions.config.model.InterfaceProxy
import com.zaneschepke.wireguardautotunnel.ui.theme.SilverTree
import com.zaneschepke.wireguardautotunnel.ui.theme.Straw
import com.zaneschepke.wireguardautotunnel.util.Constants
import com.zaneschepke.wireguardautotunnel.util.NumberUtils
import org.amnezia.awg.backend.Backend
import org.amnezia.awg.config.Config
import org.amnezia.awg.config.Interface
import timber.log.Timber
import java.net.InetAddress
import kotlin.jvm.optionals.getOrNull

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

fun Peer.isReachable(): Boolean {
	val host =
		if (this.endpoint.isPresent &&
			this.endpoint.get().resolved.isPresent
		) {
			this.endpoint.get().resolved.get().host
		} else {
			Constants.DEFAULT_PING_IP
		}
	Timber.i("Checking reachability of peer: $host")
	val reachable =
		InetAddress.getByName(host)
			.isReachable(Constants.PING_TIMEOUT.toInt())
	Timber.i("Result: reachable - $reachable")
	return reachable
}

fun TunnelStatistics?.asColor(): Color {
	return this?.mapPeerStats()
		?.map { it.value?.handshakeStatus() }
		?.let { statuses ->
			when {
				statuses.all { it == HandshakeStatus.HEALTHY } -> SilverTree
				statuses.any { it == HandshakeStatus.STALE } -> Straw
				statuses.all { it == HandshakeStatus.NOT_STARTED } -> Color.Gray
				else -> Color.Gray
			}
		} ?: Color.Gray
}

fun Config.toWgQuickString(): String {
	val amQuick = toAwgQuickString(true)
	val lines = amQuick.lines().toMutableList()
	val linesIterator = lines.iterator()
	while (linesIterator.hasNext()) {
		val next = linesIterator.next()
		Constants.amProperties.forEach {
			if (next.startsWith(it, ignoreCase = true)) {
				linesIterator.remove()
			}
		}
	}
	return lines.joinToString(System.lineSeparator())
}

fun RootShell.getCurrentWifiName(): String? {
	val response = mutableListOf<String>()
	this.run(response, "dumpsys wifi | grep 'Supplicant state: COMPLETED' | grep -o 'SSID: [^,]*' | cut -d ' ' -f2- | tr -d '\"'")
	return response.firstOrNull()
}

fun Backend.BackendState.asBackendState(): BackendState {
	return BackendState.valueOf(this.name)
}

fun BackendState.asAmBackendState(): Backend.BackendState {
	return Backend.BackendState.valueOf(this.name)
}

fun Interface.isWgCompatibilityMode(): Boolean {
	return (
		junkPacketCount.getOrNull() in 3..<5 &&
			junkPacketMinSize.getOrNull() == 40 &&
			junkPacketMaxSize.getOrNull() == 70 &&
			with(initPacketJunkSize.getOrNull()) { this == 0 || this == null } &&
			with(responsePacketJunkSize.getOrNull()) { this == 0 || this == null } &&
			initPacketMagicHeader.getOrNull() == 1L &&
			responsePacketMagicHeader.getOrNull() == 2L &&
			underloadPacketMagicHeader.getOrNull() == 3L &&
			transportPacketMagicHeader.getOrNull() == 4L
		)
}

fun InterfaceProxy.toAmneziaCompatibilityConfig(): InterfaceProxy {
	return copy(
		junkPacketCount = "4",
		junkPacketMinSize = "40",
		junkPacketMaxSize = "70",
		initPacketJunkSize = "0",
		responsePacketJunkSize = "0",
		initPacketMagicHeader = "1",
		responsePacketMagicHeader = "2",
		underloadPacketMagicHeader = "3",
		transportPacketMagicHeader = "4",
	)
}

fun InterfaceProxy.resetAmneziaProperties(): InterfaceProxy {
	return copy(
		junkPacketCount = "",
		junkPacketMinSize = "",
		junkPacketMaxSize = "",
		initPacketJunkSize = "",
		responsePacketJunkSize = "",
		initPacketMagicHeader = "",
		responsePacketMagicHeader = "",
		underloadPacketMagicHeader = "",
		transportPacketMagicHeader = "",
	)
}
