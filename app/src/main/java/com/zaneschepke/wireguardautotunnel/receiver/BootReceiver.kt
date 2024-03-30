package com.zaneschepke.wireguardautotunnel.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.zaneschepke.wireguardautotunnel.data.repository.AppDataRepository
import com.zaneschepke.wireguardautotunnel.service.foreground.ServiceManager
import com.zaneschepke.wireguardautotunnel.util.goAsync
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var appDataRepository: AppDataRepository

    @Inject
    lateinit var serviceManager: ServiceManager

    override fun onReceive(context: Context?, intent: Intent?) = goAsync {
        if (Intent.ACTION_BOOT_COMPLETED != intent?.action) return@goAsync
        context?.run {
            val settings = appDataRepository.settings.getSettings()
            if (settings.isAutoTunnelEnabled) {
                Timber.i("Starting watcher service from boot")
                serviceManager.startWatcherServiceForeground(context)
            }
            if (appDataRepository.appState.isTunnelRunningFromManualStart()) {
                appDataRepository.appState.getActiveTunnelId()?.let {
                    Timber.i("Starting tunnel that was active before reboot")
                    serviceManager.startVpnServiceForeground(
                        context,
                        appDataRepository.tunnels.getById(it)?.id,
                    )
                }
            }
            if (settings.isAlwaysOnVpnEnabled) {
                Timber.i("Starting vpn service from boot AOVPN")
                serviceManager.startVpnServiceForeground(context)
            }
        }
    }
}
