package com.zaneschepke.wireguardautotunnel.util

import com.wireguard.config.BadConfigException

class WgTunnelException(e: Exception) : Exception() {
    constructor(message: String) : this(Exception(message))

    override val message: String = generateExceptionMessage(e)

    private fun generateExceptionMessage(e: Exception): String {
        return when (e) {
            is BadConfigException -> "${e.section.name} ${e.location.name} ${e.reason.name}"
            else -> e.message ?: "Unknown error occurred"
        }
    }
}
