package com.zaneschepke.wireguardautotunnel.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.zaneschepke.wireguardautotunnel.data.repository.SettingsRepository
import com.zaneschepke.wireguardautotunnel.service.foreground.ServiceManager
import com.zaneschepke.wireguardautotunnel.util.Constants
import com.zaneschepke.wireguardautotunnel.util.goAsync
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {
    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var serviceManager: ServiceManager

    override fun onReceive(context: Context, intent: Intent?) = goAsync {
        try {
            //TODO fix for manual start changes when enabled
            serviceManager.stopVpnServiceForeground(context)
            delay(Constants.TOGGLE_TUNNEL_DELAY)
            serviceManager.startVpnServiceForeground(context)
        } catch (e: Exception) {
            Timber.e(e)
        } finally {
            cancel()
        }
    }
}
