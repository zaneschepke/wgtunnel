package com.zaneschepke.wireguardautotunnel.core.tunnel

import com.zaneschepke.wireguardautotunnel.core.network.NetworkMonitor
import com.zaneschepke.wireguardautotunnel.core.tunnel.TunnelProvider.Companion.CHECK_INTERVAL
import com.zaneschepke.wireguardautotunnel.di.ApplicationScope
import com.zaneschepke.wireguardautotunnel.di.IoDispatcher
import com.zaneschepke.wireguardautotunnel.domain.entity.TunnelConf
import com.zaneschepke.wireguardautotunnel.domain.repository.AppDataRepository
import com.zaneschepke.wireguardautotunnel.domain.state.TunnelStatistics
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

open class BaseTunnel(
	@IoDispatcher private val ioDispatcher: CoroutineDispatcher,
	@ApplicationScope private val applicationScope: CoroutineScope,
	private val networkMonitor: NetworkMonitor,
	private val appDataRepository: AppDataRepository,
) {

	internal val tunnels = MutableStateFlow<List<TunnelConf>>(emptyList())

	internal val isNetworkAvailable = AtomicBoolean(false)

	init {
		applicationScope.launch(ioDispatcher) {
			startTunnelStatisticsJob()
			startTunnelConfigChangesJob()
// 			startPingJob()
			startNetworkJob()
		}
	}

	val activeTunnels: StateFlow<List<TunnelConf>>
		get() = tunnels.asStateFlow()

	open suspend fun bounceTunnel(tunnelConf: TunnelConf) {
	}

	open suspend fun startTunnel(tunnelConf: TunnelConf) {
	}

	open suspend fun stopTunnel(tunnelConf: TunnelConf?) {
	}

	open suspend fun getStatistics(tunnelConf: TunnelConf): TunnelStatistics {
		throw NotImplementedError("Get statistics not implemented in base class")
	}

	internal suspend fun stopAllTunnels() {
		tunnels.value.forEach {
			stopTunnel(it)
		}
	}

	private fun startNetworkJob() = applicationScope.launch(ioDispatcher) {
		networkMonitor.status.distinctUntilChanged().collect {
			isNetworkAvailable.set(!it.allOffline)
		}
	}

// 	private fun startPingJob() = applicationScope.launch(ioDispatcher) {
// 		do {
// 			if(isNetworkAvailable.get()) {
// 				tunnels.value.forEach {
// 					val reachable = it.pingTunnel(ioDispatcher)
// 					if (reachable.contains(false)) {
// 						if (isNetworkAvailable.get()) {
// 							Timber.i("Ping result: target was not reachable, bouncing the tunnel")
// 							bounceTunnel(it)
// 							delay(it.pingCooldown ?: Constants.PING_COOLDOWN)
// 						} else {
// 							Timber.i("Ping result: target was not reachable, but not network available")
// 						}
// 						return@launch
// 					} else {
// 						Timber.i("Ping result: all ping targets were reached successfully")
// 					}
// 				}
// 			}
// 			delay(CHECK_INTERVAL)
// 		} while (true)
// 	}

	private fun startTunnelConfigChangesJob() = applicationScope.launch(ioDispatcher) {
		appDataRepository.tunnels.flow.collect { storageTuns ->
			tunnels.value.forEach { activeTun ->
				storageTuns.firstOrNull { it.id == activeTun.id }?.let { storageTun ->
					if (activeTun.isQuickConfigChanged(storageTun) || activeTun.isPingConfigMatching(storageTun)) {
						stopTunnel(activeTun)
						startTunnel(storageTun)
					}
				}
			}
		}
	}

	private fun startTunnelStatisticsJob() = applicationScope.launch(ioDispatcher) {
		while (true) {
			tunnels.value.forEach {
				val stats = getStatistics(it)
				it.state.update {
					it.copy(statistics = stats)
				}
			}
			delay(CHECK_INTERVAL)
		}
	}
}
