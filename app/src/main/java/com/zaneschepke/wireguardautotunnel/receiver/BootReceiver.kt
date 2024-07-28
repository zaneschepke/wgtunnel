package com.zaneschepke.wireguardautotunnel.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.zaneschepke.wireguardautotunnel.data.repository.AppDataRepository
import com.zaneschepke.wireguardautotunnel.module.ApplicationScope
import com.zaneschepke.wireguardautotunnel.service.foreground.ServiceManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var appDataRepository: AppDataRepository

    @Inject
    lateinit var serviceManager: ServiceManager

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    override fun onReceive(context: Context?, intent: Intent?) {
        if (Intent.ACTION_BOOT_COMPLETED != intent?.action) return
        context?.run {
            applicationScope.launch {
                val settings = appDataRepository.settings.getSettings()
                if (settings.isRestoreOnBootEnabled) {
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
                            return@launch
                        }
                    }
                    if (settings.isAlwaysOnVpnEnabled) {
                        Timber.i("Starting vpn service from boot AOVPN")
                        serviceManager.startVpnServiceForeground(context)
                    }
                }
            }
        }
    }
}
