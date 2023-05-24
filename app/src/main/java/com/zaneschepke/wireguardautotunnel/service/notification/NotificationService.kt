package com.zaneschepke.wireguardautotunnel.service.notification

import android.app.Notification
import android.app.NotificationManager

interface NotificationService {
    fun createNotification(
        channelId: String,
        channelName: String,
        title: String,
        description: String,
        importance: Int = NotificationManager.IMPORTANCE_HIGH,
        vibration: Boolean = true,
        lights: Boolean = true
    ): Notification
}