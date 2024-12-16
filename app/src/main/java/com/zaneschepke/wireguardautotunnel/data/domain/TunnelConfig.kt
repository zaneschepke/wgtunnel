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
	@ColumnInfo(
		name = "is_ethernet_tunnel",
		defaultValue = "false",
	)
	var isEthernetTunnel: Boolean = false,
) {

	fun toAmConfig(): org.amnezia.awg.config.Config {
		return configFromAmQuick(if (amQuick != "") amQuick else wgQuick)
	}

	fun toWgConfig(): Config {
		return configFromWgQuick(wgQuick)
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

		val IPV4_PUBLIC_NETWORKS = setOf(
			"0.0.0.0/5", "8.0.0.0/7", "11.0.0.0/8", "12.0.0.0/6", "16.0.0.0/4", "32.0.0.0/3",
			"64.0.0.0/2", "128.0.0.0/3", "160.0.0.0/5", "168.0.0.0/6", "172.0.0.0/12",
			"172.32.0.0/11", "172.64.0.0/10", "172.128.0.0/9", "173.0.0.0/8", "174.0.0.0/7",
			"176.0.0.0/4", "192.0.0.0/9", "192.128.0.0/11", "192.160.0.0/13", "192.169.0.0/16",
			"192.170.0.0/15", "192.172.0.0/14", "192.176.0.0/12", "192.192.0.0/10",
			"193.0.0.0/8", "194.0.0.0/7", "196.0.0.0/6", "200.0.0.0/5", "208.0.0.0/4",
		)
	}
}
