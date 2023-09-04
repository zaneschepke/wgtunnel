package com.zaneschepke.wireguardautotunnel.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.zaneschepke.wireguardautotunnel.repository.Repository
import com.zaneschepke.wireguardautotunnel.service.foreground.ServiceManager
import com.zaneschepke.wireguardautotunnel.service.tunnel.model.Settings
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var settingsRepo : Repository<Settings>
    override fun onReceive(context: Context, intent: Intent?) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = settingsRepo.getAll()
                if (!settings.isNullOrEmpty()) {
                    val setting = settings.first()
                    if (setting.defaultTunnel != null) {
                        ServiceManager.stopVpnService(context)
                        delay(1000)
                        ServiceManager.startVpnService(context, setting.defaultTunnel.toString())
                    }
                }
            } finally {
                cancel()
            }
        }
    }
}