package com.zaneschepke.wireguardautotunnel.service.foreground

import android.app.ActivityManager
import android.app.Application
import android.app.Service
import android.content.Context
import android.content.Context.ACTIVITY_SERVICE
import android.content.Intent
import android.content.SharedPreferences
import com.zaneschepke.wireguardautotunnel.R

object ServiceTracker {
    @Suppress("DEPRECATION")
    private // Deprecated for third party Services.
    fun <T> Context.isServiceRunning(service: Class<T>) =
        (getSystemService(ACTIVITY_SERVICE) as ActivityManager)
            .getRunningServices(Integer.MAX_VALUE)
            .any { it.service.className == service.name }

    fun <T : Service> getServiceState(context: Context, cls : Class<T>): ServiceState {
        val isServiceRunning = context.isServiceRunning(cls)
        return if(isServiceRunning) ServiceState.STARTED else ServiceState.STOPPED
    }

    fun <T : Service> actionOnService(action: Action, application: Application, cls : Class<T>, extras : Map<String,String>? = null) {
        if (getServiceState(application, cls) == ServiceState.STOPPED && action == Action.STOP) return
        val intent = Intent(application, cls).also {
            it.action = action.name
            extras?.forEach {(k, v) ->
                it.putExtra(k, v)
            }
        }
        intent.component?.javaClass
        application.startService(intent)
    }

    fun <T : Service> actionOnService(action: Action, context: Context, cls : Class<T>, extras : Map<String,String>? = null) {
        if (getServiceState(context, cls) == ServiceState.STOPPED && action == Action.STOP) return
        val intent = Intent(context, cls).also {
            it.action = action.name
            extras?.forEach {(k, v) ->
                it.putExtra(k, v)
            }
        }
        intent.component?.javaClass
        context.startService(intent)
    }
}