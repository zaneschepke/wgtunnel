package com.zaneschepke.wireguardautotunnel.service.tunnel

import com.zaneschepke.wireguardautotunnel.data.domain.TunnelConfig
import com.zaneschepke.wireguardautotunnel.service.tunnel.model.BackendState
import com.zaneschepke.wireguardautotunnel.service.tunnel.model.TunnelState
import com.zaneschepke.wireguardautotunnel.service.tunnel.model.VpnState
import com.zaneschepke.wireguardautotunnel.service.tunnel.statistics.TunnelStatistics
import com.zaneschepke.wireguardautotunnel.util.Constants
import com.zaneschepke.wireguardautotunnel.util.extensions.isReachable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import timber.log.Timber
import java.net.InetAddress
import java.util.concurrent.atomic.AtomicBoolean

abstract class TunnelService {

	internal open lateinit var tunnelConfig: TunnelConfig

	internal val state = MutableStateFlow(VpnState())

	protected val isNetworkAvailable = AtomicBoolean(false)

	abstract suspend fun startTunnel()

	abstract suspend fun stopTunnel()

	abstract suspend fun bounceTunnel()

	abstract suspend fun getBackendState(): BackendState

	abstract suspend fun setBackendState(backendState: BackendState, allowedIps: Collection<String>)

	abstract suspend fun runningTunnelNames(): Set<String>

	abstract suspend fun getState(): TunnelState

	abstract fun startActiveTunnelJobs()

	abstract fun cancelActiveTunnelJobs()

	companion object {
		const val STATS_START_DELAY = 1_000L
		const val VPN_STATISTIC_CHECK_INTERVAL = 1_000L
	}

	protected fun updateBackendStatistics(statistics: TunnelStatistics) {
		state.update {
			it.copy(statistics = statistics)
		}
	}

	protected fun resetState() {
		state.update {
			VpnState()
		}
	}

	@Synchronized
	protected fun safeUpdateConfig(tunnelConfig: TunnelConfig) {
		this.tunnelConfig = tunnelConfig
	}

	protected fun updateTunnelState(tunnelState: TunnelState) {
		state.update {
			it.copy(state = tunnelState)
		}
	}

	protected fun isQuickConfigChanged(updatedConfig: TunnelConfig): Boolean {
		return updatedConfig.wgQuick != tunnelConfig.wgQuick ||
				updatedConfig.amQuick != tunnelConfig.amQuick
	}

	protected fun isPingConfigMatching(updatedConfig: TunnelConfig) : Boolean {
		return updatedConfig.isPingEnabled == tunnelConfig.isPingEnabled &&
				tunnelConfig.pingIp == updatedConfig.pingIp &&
				updatedConfig.pingCooldown == tunnelConfig.pingCooldown &&
				updatedConfig.pingInterval == tunnelConfig.pingInterval
	}

	private fun pingTunnel(tunnelConfig: TunnelConfig): List<Boolean> {
		val config = tunnelConfig.toWgConfig()
		return if (tunnelConfig.pingIp != null) {
			Timber.i("Pinging custom ip")
			listOf(InetAddress.getByName(tunnelConfig.pingIp).isReachable(Constants.PING_TIMEOUT.toInt()))
		} else {
			Timber.i("Pinging all peers")
			config.peers.map { peer ->
				peer.isReachable(tunnelConfig.isIpv4Preferred)
			}
		}
	}

	protected suspend fun runTunnelPingCheck() {
		run {
			if (state.value.state.isUp() && isNetworkAvailable.get()) {
				val reachable = pingTunnel(tunnelConfig)
				if (reachable.contains(false)) {
					if (isNetworkAvailable.get()) {
						Timber.i("Ping result: target was not reachable, bouncing the tunnel")
						bounceTunnel()
						delay(tunnelConfig.pingCooldown ?: Constants.PING_COOLDOWN)
					} else {
						Timber.i("Ping result: target was not reachable, but not network available")
					}
					return@run
				} else {
					Timber.i("Ping result: all ping targets were reached successfully")
				}
			}
		}
		delay(tunnelConfig.pingInterval ?: Constants.PING_INTERVAL)
	}
}
