package com.zaneschepke.wireguardautotunnel.domain.entity

import android.os.Parcelable
import com.wireguard.config.Config
import com.zaneschepke.wireguardautotunnel.util.Constants
import com.zaneschepke.wireguardautotunnel.util.extensions.isReachable
import com.zaneschepke.wireguardautotunnel.util.extensions.toWgQuickString
import kotlinx.coroutines.withContext
import kotlinx.serialization.Transient
import org.amnezia.awg.backend.Tunnel
import timber.log.Timber
import java.io.InputStream
import java.net.InetAddress
import kotlin.coroutines.CoroutineContext

data class TunnelConf(
	val id: Int = 0,
	val tunName: String,
	val wgQuick: String,
	val tunnelNetworks: List<String> = emptyList(),
	val isMobileDataTunnel: Boolean = false,
	val isPrimaryTunnel: Boolean = false,
	val amQuick: String,
	val isActive: Boolean = false,
	val isPingEnabled: Boolean = false,
	val pingInterval: Long? = null,
	val pingCooldown: Long? = null,
	val pingIp: String? = null,
	val isEthernetTunnel: Boolean = false,
	val isIpv4Preferred: Boolean = false,
	@Transient
	private var stateChangeCallback: ((Any) -> Unit)? = null
) : Tunnel, com.wireguard.android.backend.Tunnel {

	fun setStateChangeCallback(callback: (Any) -> Unit) {
		stateChangeCallback = callback
	}

	fun toAmConfig(): org.amnezia.awg.config.Config {
		return configFromAmQuick(amQuick.ifBlank { wgQuick })
	}

	fun toWgConfig(): Config {
		return configFromWgQuick(wgQuick)
	}

	override fun getName(): String {
		return tunName
	}

	override fun isIpv4ResolutionPreferred(): Boolean {
		return isIpv4Preferred
	}

	override fun onStateChange(newState: com.wireguard.android.backend.Tunnel.State) {
		stateChangeCallback?.invoke(newState)
	}

	override fun onStateChange(newState: Tunnel.State) {
		stateChangeCallback?.invoke(newState)
	}

	fun isQuickConfigMatching(updatedConf: TunnelConf): Boolean {
		return updatedConf.wgQuick == wgQuick ||
			updatedConf.amQuick == amQuick
	}

	fun isPingConfigMatching(updatedConf: TunnelConf): Boolean {
		return updatedConf.isPingEnabled == isPingEnabled &&
			pingIp == updatedConf.pingIp &&
			updatedConf.pingCooldown == pingCooldown &&
			updatedConf.pingInterval == pingInterval
	}

	suspend fun isTunnelPingable(context: CoroutineContext): Boolean {
		return withContext(context) {
			val config = toWgConfig()
			if (pingIp != null) {
				return@withContext InetAddress.getByName(pingIp)
					.isReachable(Constants.PING_TIMEOUT.toInt())
			}
			Timber.i("Pinging all peers")
			config.peers.map { peer ->
				peer.isReachable(isIpv4Preferred)
			}.all { true }
		}
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

		fun tunnelConfigFromAmConfig(config: org.amnezia.awg.config.Config, name: String): TunnelConf {
			val amQuick = config.toAwgQuickString(true)
			val wgQuick = config.toWgQuickString()
			return TunnelConf(tunName = name, wgQuick = wgQuick, amQuick = amQuick)
		}

		private const val IPV6_ALL_NETWORKS = "::/0"
		private const val IPV4_ALL_NETWORKS = "0.0.0.0/0"
		val ALL_IPS = listOf(IPV4_ALL_NETWORKS, IPV6_ALL_NETWORKS)
		private val IPV4_PUBLIC_NETWORKS = listOf(
			"0.0.0.0/5", "8.0.0.0/7", "11.0.0.0/8", "12.0.0.0/6", "16.0.0.0/4", "32.0.0.0/3",
			"64.0.0.0/2", "128.0.0.0/3", "160.0.0.0/5", "168.0.0.0/6", "172.0.0.0/12",
			"172.32.0.0/11", "172.64.0.0/10", "172.128.0.0/9", "173.0.0.0/8", "174.0.0.0/7",
			"176.0.0.0/4", "192.0.0.0/9", "192.128.0.0/11", "192.160.0.0/13", "192.169.0.0/16",
			"192.170.0.0/15", "192.172.0.0/14", "192.176.0.0/12", "192.192.0.0/10",
			"193.0.0.0/8", "194.0.0.0/7", "196.0.0.0/6", "200.0.0.0/5", "208.0.0.0/4",
		)
		val LAN_BYPASS_ALLOWED_IPS = listOf(IPV6_ALL_NETWORKS) + IPV4_PUBLIC_NETWORKS
	}
}
