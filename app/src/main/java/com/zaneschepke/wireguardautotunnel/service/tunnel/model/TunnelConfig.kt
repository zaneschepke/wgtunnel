package com.zaneschepke.wireguardautotunnel.service.tunnel.model

import com.wireguard.config.Config
import io.objectbox.annotation.ConflictStrategy
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.annotation.Unique
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.InputStream


@Entity
@Serializable
data class TunnelConfig(
    @Id
    var id : Long = 0,
    @Unique(onConflict = ConflictStrategy.REPLACE)
    var name : String,
    var wgQuick : String
) {
    override fun toString(): String {
        return Json.encodeToString(serializer(), this)
    }

    companion object {
        fun from(string : String) : TunnelConfig {
            return Json.decodeFromString<TunnelConfig>(string)
        }
        fun configFromQuick(wgQuick: String): Config {
            val inputStream: InputStream = wgQuick.byteInputStream()
            val reader = inputStream.bufferedReader(Charsets.UTF_8)
            return Config.parse(reader)
        }
    }
}