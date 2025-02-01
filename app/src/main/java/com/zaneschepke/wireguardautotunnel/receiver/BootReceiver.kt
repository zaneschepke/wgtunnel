package com.zaneschepke.wireguardautotunnel.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.zaneschepke.wireguardautotunnel.data.repository.AppDataRepository
import com.zaneschepke.wireguardautotunnel.module.ApplicationScope
import com.zaneschepke.wireguardautotunnel.service.foreground.ServiceManager
import com.zaneschepke.wireguardautotunnel.service.tunnel.TunnelService
import com.zaneschepke.wireguardautotunnel.service.tunnel.model.TunnelState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Provider

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
	@Inject
	lateinit var appDataRepository: AppDataRepository

	@Inject
	@ApplicationScope
	lateinit var applicationScope: CoroutineScope

	@Inject
	lateinit var serviceManager: ServiceManager

	override fun onReceive(context: Context, intent: Intent) {
		if (Intent.ACTION_BOOT_COMPLETED != intent.action) return
		serviceManager.updateTunnelTile()
		serviceManager.updateAutoTunnelTile()
		applicationScope.launch {
			with(appDataRepository.settings.getSettings()) {
				if (isRestoreOnBootEnabled) {
					val activeTunnels = appDataRepository.tunnels.getActive()
//					val tunState = tunnelService.get().vpnState.value.status
//					if (activeTunnels.isNotEmpty() && tunState != TunnelState.UP) {
//						Timber.i("Starting previously active tunnel")
//						tunnelService.get().startTunnel(activeTunnels.first())
//					}
					if (isAutoTunnelEnabled) {
						Timber.i("Starting watcher service from boot")
						serviceManager.startAutoTunnel(true)
					}
				}
			}
		}
	}
}
