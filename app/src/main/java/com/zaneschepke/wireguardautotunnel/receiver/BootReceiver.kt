package com.zaneschepke.wireguardautotunnel.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.zaneschepke.wireguardautotunnel.goAsync
import com.zaneschepke.wireguardautotunnel.repository.SettingsDoa
import com.zaneschepke.wireguardautotunnel.service.foreground.ServiceManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.cancel

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
    @Inject
    lateinit var settingsRepo: SettingsDoa

    override fun onReceive(
        context: Context,
        intent: Intent
    ) = goAsync {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            try {
                val settings = settingsRepo.getAll()
                if (settings.isNotEmpty()) {
                    val setting = settings.first()
                    if (setting.isAutoTunnelEnabled && setting.defaultTunnel != null) {
                        ServiceManager.startWatcherService(context, setting.defaultTunnel!!)
                    }
                }
            } finally {
                cancel()
            }
        }
    }
}
