package com.zaneschepke.wireguardautotunnel.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.zaneschepke.wireguardautotunnel.data.repository.AppDataRepository
import com.zaneschepke.wireguardautotunnel.module.ApplicationScope
import com.zaneschepke.wireguardautotunnel.service.foreground.ServiceManager
import com.zaneschepke.wireguardautotunnel.service.tunnel.TunnelService
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
	lateinit var tunnelService: TunnelService

	@Inject
	lateinit var serviceManager: ServiceManager

	@Inject
	@ApplicationScope
	lateinit var applicationScope: CoroutineScope

	override fun onReceive(context: Context, intent: Intent) {
		if (Intent.ACTION_BOOT_COMPLETED != intent.action) return
		applicationScope.launch {
			val settings = appDataRepository.settings.getSettings()
			if (settings.isRestoreOnBootEnabled) {
				appDataRepository.getStartTunnelConfig()?.let {
					tunnelService.startTunnel(it)
				}
			}
			if (settings.isAutoTunnelEnabled) {
				Timber.i("Starting watcher service from boot")
				serviceManager.startWatcherServiceForeground(context)
			}
		}
	}
}
