package com.zaneschepke.wireguardautotunnel.service.foreground

import android.app.Service
import android.content.Context
import android.content.Intent
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.data.model.Settings
import com.zaneschepke.wireguardautotunnel.data.model.TunnelConfig
import timber.log.Timber

object ServiceManager {

    //    private
    //    fun <T> Context.isServiceRunning(service: Class<T>) =
    //        (getSystemService(ACTIVITY_SERVICE) as ActivityManager)
    //            .runningAppProcesses.any {
    //                it.processName == service.name
    //            }
    //
    //    fun <T : Service> getServiceState(
    //        context: Context,
    //        cls: Class<T>
    //    ): ServiceState {
    //        val isServiceRunning = context.isServiceRunning(cls)
    //        return if (isServiceRunning) ServiceState.STARTED else ServiceState.STOPPED
    //    }

    private fun <T : Service> actionOnService(
        action: Action,
        context: Context,
        cls: Class<T>,
        extras: Map<String, String>? = null
    ) {
        val intent =
            Intent(context, cls).also {
                it.action = action.name
                extras?.forEach { (k, v) -> it.putExtra(k, v) }
            }
        intent.component?.javaClass
        try {
            when (action) {
                Action.START_FOREGROUND -> {
                    context.startForegroundService(intent)
                }
                Action.START -> {
                    context.startService(intent)
                }
                Action.STOP -> context.startService(intent)
            }
        } catch (e: Exception) {
            Timber.e(e.message)
        }
    }

    fun startVpnService(context: Context, tunnelConfig: String) {
        actionOnService(
            Action.START,
            context,
            WireGuardTunnelService::class.java,
            mapOf(context.getString(R.string.tunnel_extras_key) to tunnelConfig),
        )
    }

    fun stopVpnService(context: Context) {
        Timber.d("Stopping vpn service action")
        actionOnService(
            Action.STOP,
            context,
            WireGuardTunnelService::class.java,
        )
    }

    fun startVpnServiceForeground(context: Context, tunnelConfig: String) {
        actionOnService(
            Action.START_FOREGROUND,
            context,
            WireGuardTunnelService::class.java,
            mapOf(context.getString(R.string.tunnel_extras_key) to tunnelConfig),
        )
    }

    fun startVpnServicePrimaryTunnel(context: Context, settings: Settings, fallbackTunnel: TunnelConfig? = null) {
        if(settings.defaultTunnel != null) {
            return startVpnServiceForeground(context, settings.defaultTunnel!!)
        }
        if(fallbackTunnel != null) {
            startVpnServiceForeground(context, fallbackTunnel.toString())
        }
    }

    fun startWatcherServiceForeground(
        context: Context,
    ) {
        actionOnService(
            Action.START_FOREGROUND,
            context,
            WireGuardConnectivityWatcherService::class.java,
        )
    }

    fun startWatcherService(context: Context) {
        actionOnService(
            Action.START,
            context,
            WireGuardConnectivityWatcherService::class.java,
        )
    }

    fun stopWatcherService(context: Context) {
        actionOnService(
            Action.STOP,
            context,
            WireGuardConnectivityWatcherService::class.java,
        )
    }
}
