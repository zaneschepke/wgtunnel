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
import com.zaneschepke.wireguardautotunnel.di.IoDispatcher
import com.zaneschepke.wireguardautotunnel.domain.entity.TunnelConf
import com.zaneschepke.wireguardautotunnel.domain.enums.NotificationAction
import com.zaneschepke.wireguardautotunnel.domain.repository.TunnelRepository
import com.zaneschepke.wireguardautotunnel.util.Constants
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
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

	private val isNetworkConnected = MutableStateFlow(true)

	override fun onCreate() {
		super.onCreate()
		serviceManager.backgroundService.complete(this)
	}

	override fun onBind(intent: Intent): IBinder? {
		super.onBind(intent)
		return null
	}

	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
		serviceManager.backgroundService.complete(this)
		return super.onStartCommand(intent, flags, startId)
	}

	fun start(tunnelConf: TunnelConf) {
		Timber.d("Service starting with TunnelConf instance: ${tunnelConf.hashCode()}")
		ServiceCompat.startForeground(
			this@TunnelForegroundService,
			NotificationManager.KERNEL_SERVICE_NOTIFICATION_ID,
			createNotification(tunnelConf),
			Constants.SYSTEM_EXEMPT_SERVICE_TYPE_ID,
		)
		// monitor if we have internet connectivity
		startNetworkMonitorJob()

		// job to trigger stats emit on interval
		startTunnelStatsJob(tunnelConf)

		// monitor changes to the tunnel config
		startTunnelConfChangesJob(tunnelConf)

		startPingJob(tunnelConf)
	}

	private fun startTunnelConfChangesJob(tunnelConf: TunnelConf) = lifecycleScope.launch(ioDispatcher) {
		tunnelRepo.flow
			.flowOn(ioDispatcher)
			.map { storedTunnels ->
				storedTunnels.firstOrNull { it.id == tunnelConf.id }
			}
			.filterNotNull()
			// only emit when one of these 3 values change
			.distinctUntilChanged { old, new ->
				old.tunName == new.tunName && old.wgQuick == new.wgQuick && old.amQuick == new.amQuick
			}
			.collect { storedTunnel ->
				if (tunnelConf.isTunnelConfigChanged(storedTunnel)) {
					Timber.d("Config changed for ${storedTunnel.tunName}, bouncing")
					tunnelConf.bounceTunnel(storedTunnel)
				}
			}
	}

	private fun startNetworkMonitorJob() = lifecycleScope.launch(ioDispatcher) {
		networkMonitor.networkStatusFlow
			.flowOn(ioDispatcher)
			.collectLatest { status ->
				val isAvailable = status !is NetworkStatus.Disconnected
				isNetworkConnected.value = isAvailable
				Timber.d("Network available: $status")
			}
	}

	private fun startTunnelStatsJob(tunnel: TunnelConf) = lifecycleScope.launch(ioDispatcher) {
		while (isActive) {
			tunnel.onUpdateStatistics()
			delay(STATS_DELAY)
		}
	}

	private fun startPingJob(tunnel: TunnelConf) = lifecycleScope.launch(ioDispatcher) {
		delay(PING_START_DELAY)
		while (isActive) {
			val shouldBounce = shouldBounceTunnel(tunnel)
			val delayMs = if (shouldBounce) {
				tunnel.bounceTunnel(tunnel)
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
		stopSelf()
	}

	override fun onDestroy() {
		serviceManager.backgroundService = CompletableDeferred()
		ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
		super.onDestroy()
	}

	private fun createNotification(tunnelConf: TunnelConf): Notification {
		return notificationManager.createNotification(
			WireGuardNotification.NotificationChannels.VPN,
			title = "${getString(R.string.tunnel_running)} - ${tunnelConf.tunName}",
			actions = listOf(
				notificationManager.createNotificationAction(NotificationAction.TUNNEL_OFF, tunnelConf.id),
			),
		)
	}

	companion object {
		const val STATS_DELAY = 1_000L
		const val PING_START_DELAY = 30_000L
	}
}
