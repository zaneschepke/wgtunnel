package com.zaneschepke.wireguardautotunnel.core.service

import android.app.Notification
import android.content.Intent
import android.os.IBinder
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.zaneschepke.networkmonitor.NetworkMonitor
import com.zaneschepke.networkmonitor.NetworkStatus
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.core.notification.NotificationManager
import com.zaneschepke.wireguardautotunnel.core.notification.WireGuardNotification
import com.zaneschepke.wireguardautotunnel.core.tunnel.TunnelManager
import com.zaneschepke.wireguardautotunnel.di.IoDispatcher
import com.zaneschepke.wireguardautotunnel.domain.entity.TunnelConf
import com.zaneschepke.wireguardautotunnel.domain.enums.NotificationAction
import com.zaneschepke.wireguardautotunnel.domain.enums.TunnelStatus
import com.zaneschepke.wireguardautotunnel.domain.repository.TunnelRepository
import com.zaneschepke.wireguardautotunnel.util.Constants
import com.zaneschepke.wireguardautotunnel.util.extensions.distinctByKeys
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

@AndroidEntryPoint
class TunnelForegroundService : LifecycleService() {

	@Inject
	lateinit var notificationManager: NotificationManager

	@Inject
	lateinit var serviceManager: ServiceManager

	@Inject
	lateinit var networkMonitor: NetworkMonitor

	@Inject
	@IoDispatcher
	lateinit var ioDispatcher: CoroutineDispatcher

	@Inject
	lateinit var tunnelRepo: TunnelRepository

	@Inject
	lateinit var tunnelManager: TunnelManager

	private val isNetworkConnected = MutableStateFlow(true)

	private val tunnelJobs = ConcurrentHashMap<TunnelConf, Job>()

	override fun onCreate() {
		super.onCreate()
		serviceManager.backgroundService.complete(this)
		ServiceCompat.startForeground(
			this@TunnelForegroundService,
			NotificationManager.VPN_NOTIFICATION_ID,
			onCreateNotification(),
			Constants.SYSTEM_EXEMPT_SERVICE_TYPE_ID,
		)
	}

	override fun onBind(intent: Intent): IBinder? {
		super.onBind(intent)
		return null
	}

	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
		super.onStartCommand(intent, flags, startId)
		serviceManager.backgroundService.complete(this)
		ServiceCompat.startForeground(
			this@TunnelForegroundService,
			NotificationManager.VPN_NOTIFICATION_ID,
			onCreateNotification(),
			Constants.SYSTEM_EXEMPT_SERVICE_TYPE_ID,
		)
		start()
		return START_STICKY
	}

	fun start() = lifecycleScope.launch {
		tunnelManager.activeTunnels.distinctByKeys().collect { tuns ->
			if (tuns.isEmpty() && tunnelJobs.isEmpty()) return@collect
			if (tuns.isEmpty() && tunnelJobs.isNotEmpty()) {
				return@collect tunnelJobs.forEach { (key, _) ->
					Timber.d("Stopping all tunnel jobs")
					tunnelJobs[key]?.cancel()
					tunnelJobs.remove(key)
				}
			}
			val (jobsToStop, jobsToStart) = findMissingKeys(tuns, tunnelJobs)
			if (jobsToStop.isEmpty() && jobsToStart.isEmpty()) return@collect
			jobsToStop.forEach { tun ->
				Timber.d("Stopping tunnel jobs for ${tun.tunName}")
				tunnelJobs[tun]?.cancel()
				tunnelJobs.remove(tun)
			}
			jobsToStart.forEach { tun ->
				Timber.d("Starting tunnel jobs for ${tun.tunName}")
				tunnelJobs += (tun to startTunnelJobs(tun))
			}
			updateServiceNotification()
		}
	}

	// TODO Would be cool to have this include kill switch
	// TODO also we need to include errors
	private fun updateServiceNotification() {
		val notification = when (tunnelJobs.size) {
			0 -> onCreateNotification()
			1 -> createTunnelNotification(tunnelJobs.keys.first())
			else -> createTunnelsNotification()
		}
		ServiceCompat.startForeground(
			this@TunnelForegroundService,
			NotificationManager.VPN_NOTIFICATION_ID,
			notification,
			Constants.SYSTEM_EXEMPT_SERVICE_TYPE_ID,
		)
	}

	// use same scope so we can cancel all of these
	private fun startTunnelJobs(tunnelConf: TunnelConf) = lifecycleScope.launch {
		// monitor if we have internet connectivity
		launch { startNetworkMonitorJob() }
		// job to trigger stats emit on interval
		launch { startTunnelStatsJob(tunnelConf) }
		// monitor changes to the tunnel config
		launch { startTunnelConfChangesJob(tunnelConf) }
		// monitor tunnel ping
		launch { startPingJob(tunnelConf) }
	}

	private fun findMissingKeys(map1: Map<TunnelConf, Any>, map2: Map<TunnelConf, Any>): Pair<Set<TunnelConf>, Set<TunnelConf>> {
		val missingMap1 = map2.keys - map1.keys
		val missingMap2 = map1.keys - map2.keys
		return missingMap1 to missingMap2
	}

	private suspend fun startTunnelConfChangesJob(tunnelConf: TunnelConf) {
		tunnelRepo.flow
			.flowOn(ioDispatcher)
			.map { storedTunnels ->
				storedTunnels.firstOrNull { it.id == tunnelConf.id }
			}
			.filterNotNull()
			// only emit when one of these 3 values change
			.distinctUntilChanged { old, new ->
				old == new
			}
			.collect { storedTunnel ->
				if (tunnelConf != storedTunnel) {
					Timber.d("Config changed for ${storedTunnel.tunName}, bouncing")
					// let this complete, even after cancel
					withContext(NonCancellable) {
						tunnelManager.bounceTunnel(storedTunnel, TunnelStatus.StopReason.CONFIG_CHANGED)
					}
				}
			}
	}

	private suspend fun startNetworkMonitorJob() {
		networkMonitor.networkStatusFlow
			.flowOn(ioDispatcher)
			.collectLatest { status ->
				val isAvailable = status !is NetworkStatus.Disconnected
				isNetworkConnected.value = isAvailable
				Timber.d("Network available: $status")
			}
	}

	private suspend fun startTunnelStatsJob(tunnel: TunnelConf) = coroutineScope {
		while (isActive) {
			tunnelManager.updateTunnelStatistics(tunnel)
			delay(STATS_DELAY)
		}
	}

	// TODO fix cooldown
	private suspend fun startPingJob(tunnel: TunnelConf) = coroutineScope {
		delay(PING_START_DELAY)
		while (isActive) {
			val shouldBounce = shouldBounceTunnel(tunnel)
			val delayMs = if (shouldBounce) {
				// let this complete, even after cancel
				withContext(NonCancellable) {
					tunnelManager.bounceTunnel(tunnel, TunnelStatus.StopReason.PING)
				}
				tunnel.pingCooldown ?: Constants.PING_COOLDOWN
			} else {
				tunnel.pingInterval ?: Constants.PING_INTERVAL
			}
			delay(delayMs)
		}
	}

	private suspend fun shouldBounceTunnel(tunnel: TunnelConf): Boolean {
		if (!isNetworkConnected.value) {
			Timber.d("Network disconnected, skipping ping for ${tunnel.tunName}")
			return false
		}
		return runCatching {
			!tunnel.isTunnelPingable(ioDispatcher)
		}.onFailure { e ->
			Timber.e(e, "Ping check failed for ${tunnel.tunName}")
		}.getOrDefault(true)
	}

	fun stop() {
		ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
		stopSelf()
	}

	override fun onDestroy() {
		serviceManager.backgroundService = CompletableDeferred()
		ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
		super.onDestroy()
	}

	private fun createTunnelNotification(tunnelConf: TunnelConf): Notification {
		return notificationManager.createNotification(
			WireGuardNotification.NotificationChannels.VPN,
			title = "${getString(R.string.tunnel_running)} - ${tunnelConf.tunName}",
			actions = listOf(
				notificationManager.createNotificationAction(NotificationAction.TUNNEL_OFF, tunnelConf.id),
			),
		)
	}

	private fun createTunnelsNotification(): Notification {
		return notificationManager.createNotification(
			WireGuardNotification.NotificationChannels.VPN,
			title = "${getString(R.string.tunnel_running)} - ${getString(R.string.multiple)}",
			actions = listOf(
				notificationManager.createNotificationAction(NotificationAction.TUNNEL_OFF, 0),
			),
		)
	}

	private fun onCreateNotification(): Notification {
		return notificationManager.createNotification(
			WireGuardNotification.NotificationChannels.VPN,
			title = getString(R.string.tunnel_starting),
		)
	}

	// TODO add notification handling and optional log reading for restart on handshake failures
	companion object {
		const val STATS_DELAY = 1_000L
		const val PING_START_DELAY = 30_000L
		// ipv6 disabled or block on network
// 		const val userspaceStartFailed = "Failed to send handshake initiation: write udp [::]"
// 		const val ipv6Fails = "Failed to send data packets: write udp [::]"
// 		const val ipv4Fails = "Failed to send data packets: write udp 0.0.0.0:51820"
	}
}
