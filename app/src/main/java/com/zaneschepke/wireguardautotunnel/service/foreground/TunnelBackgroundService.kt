package com.zaneschepke.wireguardautotunnel.service.foreground

import android.app.Notification
import android.content.Intent
import android.os.IBinder
import androidx.lifecycle.LifecycleService
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.service.notification.NotificationService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class TunnelBackgroundService : LifecycleService() {

	@Inject
	lateinit var notificationService: NotificationService

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
		if (intent != null) {
			val action = intent.action
			when (action) {
				Action.START.name,
				Action.START_FOREGROUND.name,
				-> startService()
				Action.STOP.name, Action.STOP_FOREGROUND.name -> stopService()
			}
		}
		return super.onStartCommand(intent, flags, startId)
	}

	private fun startService() {
		startForeground(foregroundId, createNotification())
	}

	private fun stopService() {
		stopForeground(STOP_FOREGROUND_REMOVE)
		stopSelf()
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
