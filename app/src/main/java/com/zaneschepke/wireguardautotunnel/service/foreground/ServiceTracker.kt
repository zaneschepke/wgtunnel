package com.zaneschepke.wireguardautotunnel.service.foreground

import android.app.Application
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import com.zaneschepke.wireguardautotunnel.R

object ServiceTracker {
    fun <T : Service> setServiceState(context: Context, state: ServiceState, cls : Class<T>) {
        val sharedPrefs = getPreferences(context)
        sharedPrefs.edit().let {
            it.putString(cls.simpleName, state.name)
            it.apply()
        }
    }

    private fun <T : Service> getServiceState(context: Context, cls : Class<T>): ServiceState {
        val sharedPrefs = getPreferences(context)
        val value = sharedPrefs.getString(cls.simpleName, ServiceState.STOPPED.name)
        return ServiceState.valueOf(value!!)
    }

    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(context.resources.getString(R.string.foreground_file), 0)
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