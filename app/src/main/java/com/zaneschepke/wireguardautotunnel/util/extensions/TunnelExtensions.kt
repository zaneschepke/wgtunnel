package com.zaneschepke.wireguardautotunnel.util.extensions

import androidx.compose.ui.graphics.Color
import com.wireguard.android.backend.BackendException
import com.wireguard.android.util.RootShell
import com.wireguard.config.Peer
import com.zaneschepke.wireguardautotunnel.domain.enums.BackendError
import com.zaneschepke.wireguardautotunnel.domain.enums.BackendState
import com.zaneschepke.wireguardautotunnel.domain.enums.HandshakeStatus
import com.zaneschepke.wireguardautotunnel.domain.enums.TunnelStatus
import com.zaneschepke.wireguardautotunnel.domain.enums.TunnelStatus.DOWN
import com.zaneschepke.wireguardautotunnel.domain.enums.TunnelStatus.UP
import com.zaneschepke.wireguardautotunnel.domain.state.TunnelStatistics
import com.zaneschepke.wireguardautotunnel.ui.theme.SilverTree
import com.zaneschepke.wireguardautotunnel.ui.theme.Straw
import com.zaneschepke.wireguardautotunnel.util.Constants
import com.zaneschepke.wireguardautotunnel.util.NumberUtils
import org.amnezia.awg.backend.Backend
import org.amnezia.awg.backend.Tunnel
import org.amnezia.awg.config.Config
import timber.log.Timber
import java.net.InetAddress

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

fun Peer.isReachable(preferIpv4: Boolean): Boolean {
	val host =
		if (this.endpoint.isPresent &&
			this.endpoint.get().getResolved(preferIpv4).isPresent
		) {
			this.endpoint.get().getResolved(preferIpv4).get().host
		} else {
			Constants.DEFAULT_PING_IP
		}
	Timber.d("Checking reachability of peer: $host")
	val reachable =
		InetAddress.getByName(host)
			.isReachable(Constants.PING_TIMEOUT.toInt())
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

fun Tunnel.State.asTunnelState(): TunnelStatus {
	return when (this) {
		Tunnel.State.DOWN -> DOWN
		Tunnel.State.UP -> UP
	}
}

fun BackendException.toBackendError(): BackendError {
	return when (this.reason) {
		BackendException.Reason.VPN_NOT_AUTHORIZED -> BackendError.Unauthorized
		BackendException.Reason.DNS_RESOLUTION_FAILURE -> BackendError.DNS
		else -> BackendError.Unauthorized
	}
}

fun org.amnezia.awg.backend.BackendException.toBackendError(): BackendError {
	return when (this.reason) {
		org.amnezia.awg.backend.BackendException.Reason.VPN_NOT_AUTHORIZED -> BackendError.Unauthorized
		org.amnezia.awg.backend.BackendException.Reason.DNS_RESOLUTION_FAILURE -> BackendError.DNS
		else -> BackendError.Unauthorized
	}
}

fun com.wireguard.android.backend.Tunnel.State.asTunnelState(): TunnelStatus {
	return when (this) {
		com.wireguard.android.backend.Tunnel.State.DOWN -> DOWN
		com.wireguard.android.backend.Tunnel.State.UP -> UP
	}
}
