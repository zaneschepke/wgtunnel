package com.zaneschepke.wireguardautotunnel.service.foreground

import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import androidx.lifecycle.LifecycleService
import com.zaneschepke.wireguardautotunnel.util.Constants
import timber.log.Timber

open class ForegroundService : LifecycleService() {
    private var isServiceStarted = false

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        // We don't provide binding, so return null
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Timber.d("onStartCommand executed with startId: $startId")
        if (intent != null) {
            val action = intent.action
            when (action) {
                Action.START.name,
                Action.START_FOREGROUND.name -> startService(intent.extras)
                Action.STOP.name, Action.STOP_FOREGROUND.name -> stopService()
                Constants.ALWAYS_ON_VPN_ACTION -> {
                    Timber.i("Always-on VPN starting service")
                    startService(intent.extras)
                }

                else -> Timber.d("This should never happen. No action in the received intent")
            }
        } else {
            Timber.d(
                "with a null intent. It has been probably restarted by the system.",
            )
        }
        return START_STICKY
    }

    protected open fun startService(extras: Bundle?) {
        if (isServiceStarted) return
        Timber.d("Starting ${this.javaClass.simpleName}")
        isServiceStarted = true
    }

    protected open fun stopService() {
        Timber.d("Stopping ${this.javaClass.simpleName}")
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        isServiceStarted = false
    }
}
