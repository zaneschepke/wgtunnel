package com.zaneschepke.wireguardautotunnel.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.zaneschepke.wireguardautotunnel.module.ApplicationScope
import com.zaneschepke.wireguardautotunnel.service.foreground.ServiceManager
import com.zaneschepke.wireguardautotunnel.service.notification.NotificationAction
import com.zaneschepke.wireguardautotunnel.service.tunnel.TunnelService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {

	@Inject
	lateinit var serviceManager: ServiceManager

	@Inject
	lateinit var tunnelService: TunnelService

	@Inject
	@ApplicationScope
	lateinit var applicationScope: CoroutineScope

	override fun onReceive(context: Context, intent: Intent) {
		applicationScope.launch {
			when (intent.action) {
				NotificationAction.AUTO_TUNNEL_OFF.name -> serviceManager.stopAutoTunnel()
				NotificationAction.TUNNEL_OFF.name -> tunnelService.stopTunnel()
			}
		}
	}
}
