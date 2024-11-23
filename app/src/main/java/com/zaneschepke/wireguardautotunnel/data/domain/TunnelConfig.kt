package com.zaneschepke.wireguardautotunnel.data.domain

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.wireguard.config.Config
import com.zaneschepke.wireguardautotunnel.util.extensions.toWgQuickString
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
	@ColumnInfo(
		name = "am_quick",
		defaultValue = "",
	)
	val amQuick: String = AM_QUICK_DEFAULT,
	@ColumnInfo(
		name = "is_Active",
		defaultValue = "false",
	)
	val isActive: Boolean = false,
	@ColumnInfo(
		name = "is_ping_enabled",
		defaultValue = "false",
	)
	val isPingEnabled: Boolean = false,
	@ColumnInfo(
		name = "ping_interval",
		defaultValue = "null",
	)
	val pingInterval: Long? = null,
	@ColumnInfo(
		name = "ping_cooldown",
		defaultValue = "null",
	)
	val pingCooldown: Long? = null,
	@ColumnInfo(
		name = "ping_ip",
		defaultValue = "null",
	)
	var pingIp: String? = null,
) {

	fun toAmConfig(): org.amnezia.awg.config.Config {
		return configFromAmQuick(if (amQuick != "") amQuick else wgQuick)
	}

	companion object {

		fun configFromWgQuick(wgQuick: String): Config {
			val inputStream: InputStream = wgQuick.byteInputStream()
			return inputStream.bufferedReader(Charsets.UTF_8).use {
				Config.parse(it)
			}
		}

		fun configFromAmQuick(amQuick: String): org.amnezia.awg.config.Config {
			val inputStream: InputStream = amQuick.byteInputStream()
			return inputStream.bufferedReader(Charsets.UTF_8).use {
				org.amnezia.awg.config.Config.parse(it)
			}
		}

		fun tunnelConfigFromAmConfig(config: org.amnezia.awg.config.Config, name: String): TunnelConfig {
			val amQuick = config.toAwgQuickString(true)
			val wgQuick = config.toWgQuickString()
			return TunnelConfig(name = name, wgQuick = wgQuick, amQuick = amQuick)
		}

		const val AM_QUICK_DEFAULT = ""
	}
}
