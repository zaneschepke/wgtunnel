package com.zaneschepke.wireguardautotunnel.service.foreground

import android.app.Notification
import android.content.Intent
import android.os.IBinder
import androidx.lifecycle.LifecycleService
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.service.notification.NotificationService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CompletableDeferred
import javax.inject.Inject

@AndroidEntryPoint
class TunnelBackgroundService : LifecycleService() {

	@Inject
	lateinit var notificationService: NotificationService

	@Inject
	lateinit var serviceManager: ServiceManager

	private val foregroundId = 123

	override fun onCreate() {
		super.onCreate()
		startForeground(foregroundId, createNotification())
	}

	override fun onBind(intent: Intent): IBinder? {
		super.onBind(intent)
		// We don't provide binding, so return null
		return null
	}

	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
		serviceManager.backgroundService.complete(this)
		return super.onStartCommand(intent, flags, startId)
	}

	fun start() {
		startForeground(foregroundId, createNotification())
	}

	fun stop() {
		stopForeground(STOP_FOREGROUND_REMOVE)
		stopSelf()
	}

	override fun onDestroy() {
		serviceManager.backgroundService = CompletableDeferred()
		super.onDestroy()
	}

	private fun createNotification(): Notification {
		return notificationService.createNotification(
			getString(R.string.vpn_channel_id),
			getString(R.string.vpn_channel_name),
			getString(R.string.tunnel_start_text),
			description = "",
		)
	}
}
