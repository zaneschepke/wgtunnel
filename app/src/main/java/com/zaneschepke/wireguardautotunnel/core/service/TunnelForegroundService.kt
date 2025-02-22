package com.zaneschepke.wireguardautotunnel.core.service

import android.app.Notification
import android.content.Intent
import android.os.IBinder
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.domain.enums.NotificationAction
import com.zaneschepke.wireguardautotunnel.core.notification.NotificationManager
import com.zaneschepke.wireguardautotunnel.core.notification.WireGuardNotification
import com.zaneschepke.wireguardautotunnel.domain.entity.TunnelConf
import com.zaneschepke.wireguardautotunnel.util.Constants
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CompletableDeferred
import javax.inject.Inject

@AndroidEntryPoint
class TunnelForegroundService : LifecycleService() {

	@Inject
	lateinit var notificationManager: NotificationManager

	@Inject
	lateinit var serviceManager: ServiceManager

	override fun onCreate() {
		super.onCreate()
		serviceManager.backgroundService.complete(this)
	}

	override fun onBind(intent: Intent): IBinder? {
		super.onBind(intent)
		return null
	}

	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
		super.onStartCommand(intent, flags, startId)
		serviceManager.backgroundService.complete(this)
		return START_NOT_STICKY
	}

	fun start(tunnelConf: TunnelConf) {
		ServiceCompat.startForeground(
			this@TunnelForegroundService,
			NotificationManager.KERNEL_SERVICE_NOTIFICATION_ID,
			createNotification(tunnelConf),
			Constants.SYSTEM_EXEMPT_SERVICE_TYPE_ID,
		)
	}

	fun stop() {
		stopForeground(STOP_FOREGROUND_REMOVE)
		stopSelf()
	}

	override fun onDestroy() {
		serviceManager.backgroundService = CompletableDeferred()
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
}
