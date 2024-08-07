package com.zaneschepke.wireguardautotunnel.service.shortcut

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.zaneschepke.wireguardautotunnel.data.repository.AppDataRepository
import com.zaneschepke.wireguardautotunnel.module.IoDispatcher
import com.zaneschepke.wireguardautotunnel.service.foreground.Action
import com.zaneschepke.wireguardautotunnel.service.foreground.AutoTunnelService
import com.zaneschepke.wireguardautotunnel.service.tunnel.TunnelService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class ShortcutsActivity : ComponentActivity() {
	@Inject
	lateinit var appDataRepository: AppDataRepository

	@Inject
	lateinit var tunnelService: TunnelService

	@Inject
	@IoDispatcher
	lateinit var ioDispatcher: CoroutineDispatcher

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		lifecycleScope.launch(ioDispatcher) {
			val settings = appDataRepository.settings.getSettings()
			if (settings.isShortcutsEnabled) {
				when (intent.getStringExtra(CLASS_NAME_EXTRA_KEY)) {
					LEGACY_TUNNEL_SERVICE_NAME, TunnelService::class.java.simpleName -> {
						val tunnelName = intent.getStringExtra(TUNNEL_NAME_EXTRA_KEY)
						Timber.d("Tunnel name extra: $tunnelName")
						val tunnelConfig = tunnelName?.let {
							appDataRepository.tunnels.getAll()
								.firstOrNull { it.name == tunnelName }
						} ?: appDataRepository.getPrimaryOrFirstTunnel()
						Timber.d("Shortcut action on name: ${tunnelConfig?.name}")
						tunnelConfig?.let {
							when (intent.action) {
								Action.START.name -> tunnelService.startTunnel(it)
								Action.STOP.name -> tunnelService.stopTunnel(it)
								else -> Unit
							}
						}
					}
					AutoTunnelService::class.java.simpleName, LEGACY_AUTO_TUNNEL_SERVICE_NAME -> {
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
		const val LEGACY_TUNNEL_SERVICE_NAME = "WireGuardTunnelService"
		const val LEGACY_AUTO_TUNNEL_SERVICE_NAME = "WireGuardConnectivityWatcherService"
		const val TUNNEL_NAME_EXTRA_KEY = "tunnelName"
		const val CLASS_NAME_EXTRA_KEY = "className"
	}
}
