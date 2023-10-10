package com.zaneschepke.wireguardautotunnel.util

import java.math.BigDecimal
import java.text.DecimalFormat
import java.time.Duration
import java.time.Instant

object NumberUtils {

    private const val BYTES_IN_KB = 1024L
    private val keyValidationRegex = """^[A-Za-z0-9+/]{42}[AEIMQUYcgkosw480]=${'$'}""".toRegex()

    fun bytesToKB(bytes : Long) : BigDecimal {
        return bytes.toBigDecimal().divide(BYTES_IN_KB.toBigDecimal())
    }

    fun isValidKey(key : String) : Boolean {
        return key.matches(keyValidationRegex)
    }

    fun generateRandomTunnelName() : String {
        return "tunnel${(Math.random() * 100000).toInt()}"
    }

    fun formatDecimalTwoPlaces(bigDecimal: BigDecimal) : String {
        val df = DecimalFormat("#.##")
        return df.format(bigDecimal)
    }

    fun getSecondsBetweenTimestampAndNow(epoch : Long) : Long {
        val time = Instant.ofEpochMilli(epoch)
        return Duration.between(time, Instant.now()).seconds
    }
}