package com.zaneschepke.wireguardautotunnel.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.repository.Repository
import com.zaneschepke.wireguardautotunnel.service.foreground.Action
import com.zaneschepke.wireguardautotunnel.service.foreground.ServiceTracker
import com.zaneschepke.wireguardautotunnel.service.foreground.WireGuardTunnelService
import com.zaneschepke.wireguardautotunnel.service.tunnel.model.Settings
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var settingsRepo : Repository<Settings>
    override fun onReceive(context: Context, intent: Intent?) {
        CoroutineScope(SupervisorJob()).launch {
            try {
                val settings = settingsRepo.getAll()
                if (!settings.isNullOrEmpty()) {
                    val setting = settings.first()
                    if (setting.defaultTunnel != null) {
                        ServiceTracker.actionOnService(
                            Action.STOP, context,
                            WireGuardTunnelService::class.java,
                            mapOf(
                                context.resources.getString(R.string.tunnel_extras_key) to
                                        setting.defaultTunnel!!
                            )
                        )
                        delay(1000)
                        ServiceTracker.actionOnService(
                            Action.START, context,
                            WireGuardTunnelService::class.java,
                            mapOf(
                                context.resources.getString(R.string.tunnel_extras_key) to
                                        setting.defaultTunnel!!
                            )
                        )
                    }
                }
            } finally {
                cancel()
            }
        }
    }
}