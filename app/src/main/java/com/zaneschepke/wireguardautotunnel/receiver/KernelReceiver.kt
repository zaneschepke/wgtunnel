package com.zaneschepke.wireguardautotunnel.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.zaneschepke.wireguardautotunnel.WireGuardAutoTunnel
import com.zaneschepke.wireguardautotunnel.data.repository.TunnelConfigRepository
import com.zaneschepke.wireguardautotunnel.module.ApplicationScope
import com.zaneschepke.wireguardautotunnel.service.tunnel.TunnelService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class KernelReceiver : BroadcastReceiver() {

	@Inject
	lateinit var tunnelService: TunnelService

	@Inject
	@ApplicationScope
	lateinit var applicationScope: CoroutineScope

	@Inject
	lateinit var tunnelConfigRepository: TunnelConfigRepository

	override fun onReceive(context: Context, intent: Intent) {
		val action = intent.action ?: return
		applicationScope.launch {
			if (action == REFRESH_TUNNELS_ACTION) {
				tunnelService.runningTunnelNames.forEach { name ->
					// TODO can optimize later
					val tunnel = tunnelConfigRepository.findByTunnelName(name)
					tunnel?.let {
						tunnelConfigRepository.save(it.copy(isActive = true))
						WireGuardAutoTunnel.requestTunnelTileServiceStateUpdate()
					}
				}
			}
		}
	}

	companion object {
		const val REFRESH_TUNNELS_ACTION = "com.wireguard.android.action.REFRESH_TUNNEL_STATES"
	}
}
