package com.zaneschepke.wireguardautotunnel.service.foreground

import android.app.Notification
import android.os.Bundle
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.service.notification.NotificationService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class TunnelBackgroundService : ForegroundService() {

	@Inject
	lateinit var notificationService: NotificationService

	private val foregroundId = 123

	override fun onCreate() {
		super.onCreate()
		startForeground(foregroundId, createNotification())
	}

	override fun startService(extras: Bundle?) {
		super.startService(extras)
		startForeground(foregroundId, createNotification())
	}

	override fun stopService() {
		super.stopService()
		stopForeground(STOP_FOREGROUND_REMOVE)
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
