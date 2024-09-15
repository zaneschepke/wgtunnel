package com.zaneschepke.wireguardautotunnel.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.zaneschepke.wireguardautotunnel.data.repository.AppDataRepository
import com.zaneschepke.wireguardautotunnel.module.ApplicationScope
import com.zaneschepke.wireguardautotunnel.service.foreground.ServiceManager
import com.zaneschepke.wireguardautotunnel.service.tunnel.TunnelService
import com.zaneschepke.wireguardautotunnel.util.extensions.startTunnelBackground
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class AppUpdateReceiver : BroadcastReceiver() {

	@Inject
	@ApplicationScope
	lateinit var applicationScope: CoroutineScope

	@Inject
	lateinit var appDataRepository: AppDataRepository

	@Inject
	lateinit var tunnelService: TunnelService

	override fun onReceive(context: Context, intent: Intent) {
		if (intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
		applicationScope.launch {
			val settings = appDataRepository.settings.getSettings()
			if (settings.isAutoTunnelEnabled) {
				Timber.i("Restarting services after upgrade")
				ServiceManager.startWatcherServiceForeground(context)
			}
			if (!settings.isAutoTunnelEnabled || settings.isAutoTunnelPaused) {
				val tunnels = appDataRepository.tunnels.getAll().filter { it.isActive }
				if (tunnels.isNotEmpty()) context.startTunnelBackground(tunnels.first().id)
			}
		}
	}
}
