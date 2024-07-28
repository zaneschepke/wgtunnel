package com.zaneschepke.wireguardautotunnel.service.shortcut

import android.os.Bundle
import androidx.activity.ComponentActivity
import com.zaneschepke.wireguardautotunnel.data.repository.AppDataRepository
import com.zaneschepke.wireguardautotunnel.module.ApplicationScope
import com.zaneschepke.wireguardautotunnel.service.foreground.Action
import com.zaneschepke.wireguardautotunnel.service.foreground.ServiceManager
import com.zaneschepke.wireguardautotunnel.service.foreground.WireGuardConnectivityWatcherService
import com.zaneschepke.wireguardautotunnel.service.foreground.WireGuardTunnelService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ShortcutsActivity : ComponentActivity() {
	@Inject
	lateinit var appDataRepository: AppDataRepository

	@Inject
	lateinit var serviceManager: ServiceManager

	@Inject
	@ApplicationScope
	lateinit var applicationScope: CoroutineScope

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		applicationScope.launch {
			val settings = appDataRepository.settings.getSettings()
			if (settings.isShortcutsEnabled) {
				when (intent.getStringExtra(CLASS_NAME_EXTRA_KEY)) {
					WireGuardTunnelService::class.java.simpleName -> {
						val tunnelName = intent.getStringExtra(TUNNEL_NAME_EXTRA_KEY)
						val tunnelConfig =
							tunnelName?.let {
								appDataRepository.tunnels.getAll().firstOrNull {
									it.name == tunnelName
								}
							}
						when (intent.action) {
							Action.START.name ->
								serviceManager.startVpnServiceForeground(
									this@ShortcutsActivity,
									tunnelConfig?.id,
									isManualStart = true,
								)

							Action.STOP.name ->
								serviceManager.stopVpnServiceForeground(
									this@ShortcutsActivity,
									isManualStop = true,
								)
						}
					}

					WireGuardConnectivityWatcherService::class.java.simpleName -> {
						when (intent.action) {
							Action.START.name ->
								appDataRepository.settings.save(
									settings.copy(
										isAutoTunnelPaused = false,
									),
								)

							Action.STOP.name ->
								appDataRepository.settings.save(
									settings.copy(
										isAutoTunnelPaused = true,
									),
								)
						}
					}
				}
			}
		}
		finish()
	}

	companion object {
		const val TUNNEL_NAME_EXTRA_KEY = "tunnelName"
		const val CLASS_NAME_EXTRA_KEY = "className"
	}
}
