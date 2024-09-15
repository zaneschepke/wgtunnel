package com.zaneschepke.wireguardautotunnel.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.zaneschepke.wireguardautotunnel.data.repository.TunnelConfigRepository
import com.zaneschepke.wireguardautotunnel.module.ApplicationScope
import com.zaneschepke.wireguardautotunnel.service.foreground.ServiceManager
import com.zaneschepke.wireguardautotunnel.service.tunnel.TunnelService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Provider

@AndroidEntryPoint
class BackgroundActionReceiver : BroadcastReceiver() {

	@Inject
	@ApplicationScope
	lateinit var applicationScope: CoroutineScope

	@Inject
	lateinit var tunnelService: Provider<TunnelService>

	@Inject
	lateinit var tunnelConfigRepository: TunnelConfigRepository

	override fun onReceive(context: Context, intent: Intent) {
		val id = intent.getIntExtra(TUNNEL_ID_EXTRA_KEY, 0)
		if (id == 0) return
		when (intent.action) {
			ACTION_CONNECT -> {
				Timber.d("Connect actions")
				applicationScope.launch {
					val tunnel = tunnelConfigRepository.getById(id)
					tunnel?.let {
						ServiceManager.startTunnelBackgroundService(context)
						tunnelService.get().startTunnel(it)
					}
				}
			}
			ACTION_DISCONNECT -> {
				applicationScope.launch {
					val tunnel = tunnelConfigRepository.getById(id)
					tunnel?.let {
						ServiceManager.stopTunnelBackgroundService(context)
						tunnelService.get().stopTunnel(it)
					}
				}
			}
		}
	}

	companion object {
		const val ACTION_CONNECT = "ACTION_CONNECT"
		const val ACTION_DISCONNECT = "ACTION_DISCONNECT"
		const val TUNNEL_ID_EXTRA_KEY = "tunnelId"
	}
}
