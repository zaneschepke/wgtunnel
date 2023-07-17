package com.zaneschepke.wireguardautotunnel.service.notification

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent

interface NotificationService {
    fun createNotification(
        channelId: String,
        channelName: String,
        title: String = "",
        action: PendingIntent? = null,
        actionText: String? = null,
        description: String,
        showTimestamp : Boolean = false,
        importance: Int = NotificationManager.IMPORTANCE_HIGH,
        vibration: Boolean = true,
        onGoing: Boolean = true,
        lights: Boolean = true
    ): Notification
}