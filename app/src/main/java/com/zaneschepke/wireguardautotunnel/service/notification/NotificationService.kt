package com.zaneschepke.wireguardautotunnel.service.notification

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.zaneschepke.wireguardautotunnel.service.notification.WireGuardNotification.NotificationChannels

interface NotificationService {
	val context: Context
	fun createNotification(
		channel: NotificationChannels,
		title: String = "",
		actions: Collection<NotificationCompat.Action> = emptyList(),
		description: String = "",
		showTimestamp: Boolean = false,
		importance: Int = NotificationManager.IMPORTANCE_HIGH,
		onGoing: Boolean = true,
		onlyAlertOnce: Boolean = true,
	): Notification

	fun createNotificationAction(action: NotificationAction): NotificationCompat.Action

	fun remove(notificationId: Int)

	fun show(notificationId: Int, notification: Notification)

	companion object {
		const val KERNEL_SERVICE_NOTIFICATION_ID = 123
		const val AUTO_TUNNEL_NOTIFICATION_ID = 122
		const val VPN_NOTIFICATION_ID = 100
	}
}
