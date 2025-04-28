package com.zaneschepke.wireguardautotunnel.domain.enums

sealed class TunnelStatus {

    data object Up : TunnelStatus()

    data object Down : TunnelStatus()

    data class Stopping(val reason: StopReason) : TunnelStatus()

    data object Starting : TunnelStatus()

    enum class StopReason {
        USER,
        PING,
        CONFIG_CHANGED,
    }

    fun isDown(): Boolean {
        return this == Down
    }

    fun isUp(): Boolean {
        return this == Up
    }

    fun isUpOrStarting(): Boolean {
        return this == Up || this == Starting
    }

    fun isDownOrStopping(): Boolean {
        return this == Down || this is Stopping
    }
}
