package com.zaneschepke.wireguardautotunnel.domain.enums

enum class HandshakeStatus {
    HEALTHY,
    STALE,
    UNKNOWN,
    NOT_STARTED;

    companion object {
        private const val WG_TYPICAL_HANDSHAKE_INTERVAL_WHEN_HEALTHY_SEC = 180
        const val STATUS_CHANGE_TIME_BUFFER = 30
        const val STALE_TIME_LIMIT_SEC =
            WG_TYPICAL_HANDSHAKE_INTERVAL_WHEN_HEALTHY_SEC + STATUS_CHANGE_TIME_BUFFER
        const val NEVER_CONNECTED_TO_UNHEALTHY_TIME_LIMIT_SEC = 30
    }
}
