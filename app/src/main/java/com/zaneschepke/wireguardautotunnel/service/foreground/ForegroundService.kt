package com.zaneschepke.wireguardautotunnel.service.foreground

import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import com.zaneschepke.wireguardautotunnel.repository.Repository
import com.zaneschepke.wireguardautotunnel.service.tunnel.model.Settings
import timber.log.Timber
import javax.inject.Inject


open class ForegroundService : Service() {

    private var isServiceStarted = false

    override fun onBind(intent: Intent): IBinder? {
        // We don't provide binding, so return null
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.d("onStartCommand executed with startId: $startId")
        if (intent != null) {
            val action = intent.action
            Timber.d("using an intent with action $action")
            when (action) {
                Action.START.name -> startService(intent.extras)
                Action.STOP.name -> stopService(intent.extras)
                "android.net.VpnService" -> {
                    Timber.d("Always-on VPN starting service")
                    startService(intent.extras)
                }
                else -> Timber.d("This should never happen. No action in the received intent")
            }
        } else {
            Timber.d(
                "with a null intent. It has been probably restarted by the system."
            )
        }
        // by returning this we make sure the service is restarted if the system kills the service
        return START_STICKY
    }


    override fun onDestroy() {
        super.onDestroy()
        Timber.d("The service has been destroyed")
    }

    protected open fun startService(extras : Bundle?) {
        if (isServiceStarted) return
        Timber.d("Starting ${this.javaClass.simpleName}")
        isServiceStarted = true
    }

    protected open fun stopService(extras : Bundle?) {
        Timber.d("Stopping ${this.javaClass.simpleName}")
        try {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        } catch (e: Exception) {
            Timber.d("Service stopped without being started: ${e.message}")
        }
        isServiceStarted = false
    }
}