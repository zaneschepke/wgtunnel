package com.zaneschepke.wireguardautotunnel.core.notification

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.zaneschepke.wireguardautotunnel.core.notification.WireGuardNotification.NotificationChannels
import com.zaneschepke.wireguardautotunnel.domain.enums.NotificationAction
import com.zaneschepke.wireguardautotunnel.util.StringValue

interface NotificationManager {
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

    fun createNotification(
        channel: NotificationChannels,
        title: StringValue,
        actions: Collection<NotificationCompat.Action> = emptyList(),
        description: StringValue,
        showTimestamp: Boolean = false,
        importance: Int = NotificationManager.IMPORTANCE_HIGH,
        onGoing: Boolean = true,
        onlyAlertOnce: Boolean = true,
    ): Notification

    fun createNotificationAction(
        notificationAction: NotificationAction,
        extraId: Int? = null,
    ): NotificationCompat.Action

    fun remove(notificationId: Int)

    fun show(notificationId: Int, notification: Notification)

    companion object {
        const val AUTO_TUNNEL_NOTIFICATION_ID = 122
        const val VPN_NOTIFICATION_ID = 100
        const val EXTRA_ID = "id"
    }
}
