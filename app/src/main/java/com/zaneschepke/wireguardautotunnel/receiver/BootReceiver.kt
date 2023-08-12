package com.zaneschepke.wireguardautotunnel.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.repository.Repository
import com.zaneschepke.wireguardautotunnel.service.foreground.Action
import com.zaneschepke.wireguardautotunnel.service.foreground.ServiceTracker
import com.zaneschepke.wireguardautotunnel.service.foreground.WireGuardConnectivityWatcherService
import com.zaneschepke.wireguardautotunnel.service.tunnel.model.Settings
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var settingsRepo : Repository<Settings>

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val settings = settingsRepo.getAll()
                    if (!settings.isNullOrEmpty()) {
                        val setting = settings.first()
                        if (setting.isAutoTunnelEnabled && setting.defaultTunnel != null) {
                            ServiceTracker.actionOnService(
                                Action.START, context,
                                WireGuardConnectivityWatcherService::class.java,
                                mapOf(context.resources.getString(R.string.tunnel_extras_key) to
                                setting.defaultTunnel!!)
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