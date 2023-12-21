package com.zaneschepke.wireguardautotunnel.util

import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import kotlin.math.pow

object NumberUtils {
    private const val BYTES_IN_KB = 1024.0
    private val BYTES_IN_MB = BYTES_IN_KB.pow(2.0)
    private val keyValidationRegex = """^[A-Za-z0-9+/]{42}[AEIMQUYcgkosw480]=${'$'}""".toRegex()

    fun bytesToMB(bytes: Long): BigDecimal {
        return bytes.toBigDecimal().divide(BYTES_IN_MB.toBigDecimal())
    }

    fun isValidKey(key: String): Boolean {
        return key.matches(keyValidationRegex)
    }

    fun generateRandomTunnelName(): String {
        return "tunnel${(Math.random() * 100000).toInt()}"
    }

    fun getSecondsBetweenTimestampAndNow(epoch: Long): Long? {
        return if (epoch != 0L) {
            val time = Instant.ofEpochMilli(epoch)
            return Duration.between(time, Instant.now()).seconds
        } else {
            null
        }
    }
}
