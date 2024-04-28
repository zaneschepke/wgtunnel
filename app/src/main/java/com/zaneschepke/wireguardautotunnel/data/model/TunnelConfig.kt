package com.zaneschepke.wireguardautotunnel.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import org.amnezia.awg.config.Config
import java.io.InputStream

@Entity(indices = [Index(value = ["name"], unique = true)])
data class TunnelConfig(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "wg_quick") val wgQuick: String,
    @ColumnInfo(
        name = "tunnel_networks",
        defaultValue = "",
    )
    val tunnelNetworks: MutableList<String> = mutableListOf(),
    @ColumnInfo(
        name = "is_mobile_data_tunnel",
        defaultValue = "false",
    )
    val isMobileDataTunnel: Boolean = false,
    @ColumnInfo(
        name = "is_primary_tunnel",
        defaultValue = "false",
    )
    val isPrimaryTunnel: Boolean = false,
) {
    companion object {
        fun configFromQuick(wgQuick: String): Config {
            val inputStream: InputStream = wgQuick.byteInputStream()
            val reader = inputStream.bufferedReader(Charsets.UTF_8)
            return Config.parse(reader)
        }
    }
}
