package com.zaneschepke.wireguardautotunnel.domain.state

import org.amnezia.awg.crypto.Key

abstract class TunnelStatistics {
    @JvmRecord
    data class PeerStats(
        val rxBytes: Long,
        val txBytes: Long,
        val latestHandshakeEpochMillis: Long,
    )

    abstract fun peerStats(peer: Key): PeerStats?

    abstract fun isTunnelStale(): Boolean

    abstract fun getPeers(): Array<Key>

    abstract fun rx(): Long

    abstract fun tx(): Long
}
