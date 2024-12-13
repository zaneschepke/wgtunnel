package com.zaneschepke.wireguardautotunnel.service.foreground

import android.app.Notification
import android.content.Intent
import android.os.IBinder
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.data.domain.TunnelConfig
import com.zaneschepke.wireguardautotunnel.service.notification.NotificationAction
import com.zaneschepke.wireguardautotunnel.service.notification.NotificationService
import com.zaneschepke.wireguardautotunnel.service.notification.WireGuardNotification
import com.zaneschepke.wireguardautotunnel.util.Constants
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CompletableDeferred
import javax.inject.Inject

@AndroidEntryPoint
class TunnelBackgroundService : LifecycleService() {

	@Inject
	lateinit var notificationService: NotificationService

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
		serviceManager.backgroundService.complete(this)
		return super.onStartCommand(intent, flags, startId)
	}

	fun start(tunnelConfig: TunnelConfig?) {
		ServiceCompat.startForeground(
			this,
			NotificationService.KERNEL_SERVICE_NOTIFICATION_ID,
			createNotification(tunnelConfig),
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

	private fun createNotification(tunnelConfig: TunnelConfig?): Notification {
		return notificationService.createNotification(
			WireGuardNotification.NotificationChannels.VPN,
			title = "${getString(R.string.tunnel_running)} - ${tunnelConfig?.name}",
			actions = listOf(
				notificationService.createNotificationAction(NotificationAction.TUNNEL_OFF),
			),
		)
	}
}
