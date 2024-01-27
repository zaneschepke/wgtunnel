package com.zaneschepke.wireguardautotunnel.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.zaneschepke.wireguardautotunnel.data.repository.SettingsRepository
import com.zaneschepke.wireguardautotunnel.data.repository.TunnelConfigRepository
import com.zaneschepke.wireguardautotunnel.service.foreground.ServiceManager
import com.zaneschepke.wireguardautotunnel.util.goAsync
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var settingsRepository: SettingsRepository

    @Inject lateinit var tunnelConfigRepository: TunnelConfigRepository

    override fun onReceive(context: Context?, intent: Intent?) = goAsync {
        if (Intent.ACTION_BOOT_COMPLETED != intent?.action) return@goAsync
        val settings = settingsRepository.getSettings()
        if (settings.isAutoTunnelEnabled) {
            ServiceManager.startWatcherServiceForeground(context!!)
        } else if(settings.isAlwaysOnVpnEnabled) {
            ServiceManager.startVpnServicePrimaryTunnel(context!!, settings, tunnelConfigRepository.getAll().firstOrNull())
        }
    }
}
