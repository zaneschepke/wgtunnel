package com.zaneschepke.wireguardautotunnel.repository.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.wireguard.config.Config
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.InputStream

@Entity(indices = [Index(value = ["name"], unique = true)])
@Serializable
data class TunnelConfig(
    @PrimaryKey(autoGenerate = true) val id : Int = 0,
    @ColumnInfo(name = "name") var name : String,
    @ColumnInfo(name = "wg_quick") var wgQuick : String,
){

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