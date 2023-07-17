package com.zaneschepke.wireguardautotunnel.service.tunnel

enum class HandshakeStatus {
    HEALTHY,
    UNHEALTHY,
    NEVER_CONNECTED,
    NOT_STARTED;

    companion object {
        private const val WG_TYPICAL_HANDSHAKE_INTERVAL_WHEN_HEALTHY_SEC = 120
        const val UNHEALTHY_TIME_LIMIT_SEC = WG_TYPICAL_HANDSHAKE_INTERVAL_WHEN_HEALTHY_SEC + 60
        const val NEVER_CONNECTED_TO_UNHEALTHY_TIME_LIMIT_SEC = 30
    }
}