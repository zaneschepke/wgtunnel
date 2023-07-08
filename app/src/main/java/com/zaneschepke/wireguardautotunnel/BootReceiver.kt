package com.zaneschepke.wireguardautotunnel

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.zaneschepke.wireguardautotunnel.repository.Repository
import com.zaneschepke.wireguardautotunnel.service.foreground.Action
import com.zaneschepke.wireguardautotunnel.service.foreground.ServiceTracker
import com.zaneschepke.wireguardautotunnel.service.foreground.WireGuardConnectivityWatcherService
import com.zaneschepke.wireguardautotunnel.service.tunnel.model.Settings
import com.zaneschepke.wireguardautotunnel.service.tunnel.model.TunnelConfig
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var settingsRepo : Repository<Settings>

    @OptIn(DelicateCoroutinesApi::class)
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            CoroutineScope(SupervisorJob()).launch {
                try {
                    val settings = settingsRepo.getAll()
                    if (!settings.isNullOrEmpty()) {
                        val setting = settings[0]
                        if (setting.isAutoTunnelEnabled && setting.defaultTunnel != null) {
                            val defaultTunnel = TunnelConfig.from(setting.defaultTunnel!!)
                            ServiceTracker.actionOnService(
                                Action.START, context,
                                WireGuardConnectivityWatcherService::class.java,
                                mapOf(context.resources.getString(R.string.tunnel_extras_key) to
                                defaultTunnel.toString())
                            )
                        }
                    }
                } finally {
                    cancel()
                }
            }
        }
    }
}