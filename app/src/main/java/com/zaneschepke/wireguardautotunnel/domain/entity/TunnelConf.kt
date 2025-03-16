package com.zaneschepke.wireguardautotunnel.domain.entity

import com.wireguard.android.backend.Tunnel
import com.wireguard.config.Config
import com.zaneschepke.wireguardautotunnel.util.Constants
import com.zaneschepke.wireguardautotunnel.util.extensions.isReachable
import com.zaneschepke.wireguardautotunnel.util.extensions.toWgQuickString
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.InputStream
import java.net.InetAddress
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.nio.charset.StandardCharsets

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
	val isIpv4Preferred: Boolean = true,
	@Transient
	private var stateChangeCallback: ((Any) -> Unit)? = null,
) : Tunnel, org.amnezia.awg.backend.Tunnel {

	// Mutex to protect stateChangeCallback access
	private val callbackMutex = Mutex()

	suspend fun setStateChangeCallback(callback: (Any) -> Unit) {
		callbackMutex.withLock {
			stateChangeCallback = callback
		}
	}

	// Ensure callback is copied over
	fun copyWithCallback(
		id: Int = this.id,
		tunName: String = this.tunName,
		wgQuick: String = this.wgQuick,
		tunnelNetworks: List<String> = this.tunnelNetworks,
		isMobileDataTunnel: Boolean = this.isMobileDataTunnel,
		isPrimaryTunnel: Boolean = this.isPrimaryTunnel,
		amQuick: String = this.amQuick,
		isActive: Boolean = this.isActive,
		isPingEnabled: Boolean = this.isPingEnabled,
		pingInterval: Long? = this.pingInterval,
		pingCooldown: Long? = this.pingCooldown,
		pingIp: String? = this.pingIp,
		isEthernetTunnel: Boolean = this.isEthernetTunnel,
		isIpv4Preferred: Boolean = this.isIpv4Preferred,
	): TunnelConf {
		return TunnelConf(
			id, tunName, wgQuick, tunnelNetworks, isMobileDataTunnel, isPrimaryTunnel,
			amQuick, isActive, isPingEnabled, pingInterval, pingCooldown, pingIp,
			isEthernetTunnel, isIpv4Preferred,
		).apply {
			stateChangeCallback = this@TunnelConf.stateChangeCallback
		}
	}

	fun toAmConfig(): org.amnezia.awg.config.Config {
		return configFromAmQuick(amQuick.ifBlank { wgQuick })
	}

	fun toWgConfig(): Config {
		return configFromWgQuick(wgQuick)
	}

	override fun getName(): String = tunName

	override fun isIpv4ResolutionPreferred(): Boolean = isIpv4Preferred

	override fun onStateChange(newState: org.amnezia.awg.backend.Tunnel.State) {
		Timber.d("onStateChange called for tunnel $id: $tunName with state $newState")
		runBlocking {
			callbackMutex.withLock {
				if (stateChangeCallback != null) {
					Timber.d("Invoking stateChangeCallback for tunnel $id: $tunName with state $newState")
					stateChangeCallback?.invoke(newState)
				} else {
					Timber.w("No stateChangeCallback set for tunnel $id: $tunName")
				}
			}
		}
	}

	override fun onStateChange(newState: Tunnel.State) {
		Timber.d("onStateChange called for tunnel $id: $tunName with state $newState")
		runBlocking {
			callbackMutex.withLock {
				if (stateChangeCallback != null) {
					Timber.d("Invoking stateChangeCallback for tunnel $id: $tunName with state $newState")
					stateChangeCallback?.invoke(newState)
				} else {
					Timber.w("No stateChangeCallback set for tunnel $id: $tunName")
				}
			}
		}
	}

	fun isQuickConfigMatching(updatedConf: TunnelConf): Boolean {
		return updatedConf.wgQuick == wgQuick || updatedConf.amQuick == amQuick
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
			config.peers.map { peer ->
				peer.isReachable(isIpv4Preferred)
			}.all { true }
		}
	}

	companion object {
		fun configFromWgQuick(wgQuick: String): Config {
			val inputStream: InputStream = wgQuick.byteInputStream()
			return inputStream.bufferedReader(StandardCharsets.UTF_8).use {
				Config.parse(it)
			}
		}

		fun configFromAmQuick(amQuick: String): org.amnezia.awg.config.Config {
			val inputStream: InputStream = amQuick.byteInputStream()
			return inputStream.bufferedReader(StandardCharsets.UTF_8).use {
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
