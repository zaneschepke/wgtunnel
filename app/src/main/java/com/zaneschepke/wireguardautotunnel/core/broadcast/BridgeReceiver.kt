package com.zaneschepke.wireguardautotunnel.core.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.zaneschepke.wireguardautotunnel.core.service.ServiceManager
import com.zaneschepke.wireguardautotunnel.core.tunnel.TunnelManager
import com.zaneschepke.wireguardautotunnel.di.ApplicationScope
import com.zaneschepke.wireguardautotunnel.domain.repository.AppDataRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BridgeReceiver : BroadcastReceiver() {

	@Inject
	@ApplicationScope
	lateinit var applicationScope: CoroutineScope

	@Inject
	lateinit var appDataRepository: AppDataRepository

	@Inject
	lateinit var serviceManager: ServiceManager

	@Inject
	lateinit var tunnelManager: TunnelManager

	enum class Action(private val suffix: String) {
		START_TUNNEL("START_TUNNEL"),
		STOP_TUNNEL("STOP_TUNNEL"),
		START_AUTO_TUNNEL("START_AUTO_TUNNEL"),
		STOP_AUTO_TUNNEL("STOP_AUTO_TUNNEL"),
		;

		fun getFullAction(context: Context): String {
			return context.packageName + "." + suffix
		}

		companion object {
			fun fromAction(context: Context, action: String): Action? {
				for (a in entries) {
					if (a.getFullAction(context) == action) {
						return a
					}
				}
				return null
			}
		}
	}

	override fun onReceive(context: Context, intent: Intent) {
		val action = intent.action ?: return
		val appAction = Action.fromAction(context, action) ?: return
		applicationScope.launch {
			when (appAction) {
				Action.START_TUNNEL -> {
					val tunnelName = intent.getStringExtra(EXTRA) ?: return@launch startDefaultTunnel()
					val tunnel = appDataRepository.tunnels.findByTunnelName(tunnelName) ?: return@launch startDefaultTunnel()
					tunnelManager.startTunnel(tunnel)
				}
				Action.STOP_TUNNEL -> {
					val tunnelName = intent.getStringExtra(EXTRA) ?: return@launch tunnelManager.stopTunnel()
					val tunnel = appDataRepository.tunnels.findByTunnelName(tunnelName) ?: return@launch tunnelManager.stopTunnel()
					tunnelManager.stopTunnel(tunnel)
				}
				Action.START_AUTO_TUNNEL -> serviceManager.startAutoTunnel()
				Action.STOP_AUTO_TUNNEL -> serviceManager.stopAutoTunnel()
			}
		}
	}

	private suspend fun startDefaultTunnel() {
		appDataRepository.getPrimaryOrFirstTunnel()?.let { tunnel ->
			tunnelManager.startTunnel(tunnel)
		}
	}

	companion object {
		const val EXTRA = "name"
	}
}
