package com.zaneschepke.wireguardautotunnel.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.zaneschepke.wireguardautotunnel.Constants
import com.zaneschepke.wireguardautotunnel.goAsync
import com.zaneschepke.wireguardautotunnel.repository.SettingsDoa
import com.zaneschepke.wireguardautotunnel.service.foreground.ServiceManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import javax.inject.Inject

@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var settingsRepo : SettingsDoa
    override fun onReceive(context: Context, intent: Intent?) = goAsync {
        try {
            val settings = settingsRepo.getAll()
            if (settings.isNotEmpty()) {
                val setting = settings.first()
                if (setting.defaultTunnel != null) {
                    ServiceManager.stopVpnService(context)
                    delay(Constants.TOGGLE_TUNNEL_DELAY)
                    ServiceManager.startVpnService(context, setting.defaultTunnel.toString())
                }
            }
        } finally {
            cancel()
        }
    }
}