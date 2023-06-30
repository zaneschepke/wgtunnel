package com.zaneschepke.wireguardautotunnel.service.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class WireGuardNotification @Inject constructor(@ApplicationContext private val context: Context) : NotificationService {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager;

    override fun createNotification(
        channelId: String,
        channelName: String,
        title: String,
        description: String,
        showTimestamp: Boolean,
        importance: Int,
        vibration: Boolean,
        onGoing: Boolean,
        lights: Boolean
    ) : Notification {
        val channel = NotificationChannel(
            channelId,
            channelName,
            importance
        ).let {
            it.description = title
            it.enableLights(lights)
            it.lightColor = Color.RED
            it.enableVibration(vibration)
            it.vibrationPattern = longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400)
            it
        }
        notificationManager.createNotificationChannel(channel)
        val pendingIntent: PendingIntent =
            Intent(context, MainActivity::class.java).let { notificationIntent ->
                PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)
            }

        val builder: Notification.Builder =
            Notification.Builder(
                context,
                channelId
            )

        return builder
            .setContentTitle(title)
            .setContentText(description)
            .setContentIntent(pendingIntent)
            .setOngoing(onGoing)
            .setShowWhen(showTimestamp)
            .setSmallIcon(R.mipmap.ic_launcher_foreground)
            .build()
    }
}