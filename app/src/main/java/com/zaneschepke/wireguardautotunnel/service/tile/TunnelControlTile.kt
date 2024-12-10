package com.zaneschepke.wireguardautotunnel.service.tile

import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.zaneschepke.wireguardautotunnel.data.domain.TunnelConfig
import com.zaneschepke.wireguardautotunnel.data.repository.AppDataRepository
import com.zaneschepke.wireguardautotunnel.module.ApplicationScope
import com.zaneschepke.wireguardautotunnel.service.foreground.ServiceManager
import com.zaneschepke.wireguardautotunnel.service.tunnel.TunnelService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider

@AndroidEntryPoint
class TunnelControlTile : TileService() {
	@Inject
	lateinit var appDataRepository: AppDataRepository

	@Inject
	lateinit var tunnelService: Provider<TunnelService>

	@Inject
	@ApplicationScope
	lateinit var applicationScope: CoroutineScope

	@Inject
	lateinit var serviceManager: ServiceManager

	override fun onCreate() {
		super.onCreate()
		serviceManager.tunnelControlTile.complete(this)
	}

	override fun onDestroy() {
		super.onDestroy()
		serviceManager.tunnelControlTile = CompletableDeferred()
	}

	override fun onStartListening() {
		super.onStartListening()
		serviceManager.tunnelControlTile.complete(this)
		applicationScope.launch {
			if (appDataRepository.tunnels.getAll().isEmpty()) return@launch setUnavailable()
			updateTileState()
		}
	}

	fun updateTileState() = applicationScope.launch {
		val lastActive = appDataRepository.getStartTunnelConfig()
		lastActive?.let {
			updateTile(it)
		}
	}

	override fun onClick() {
		super.onClick()
		unlockAndRun {
			applicationScope.launch {
				val lastActive = appDataRepository.getStartTunnelConfig()
				lastActive?.let { tunnel ->
					if (tunnel.isActive) {
						tunnelService.get().stopTunnel()
					} else {
						tunnelService.get().startTunnel(tunnel, true)
					}
					updateTileState()
				}
			}
		}
	}

	private fun setActive() {
		kotlin.runCatching {
			qsTile.state = Tile.STATE_ACTIVE
			qsTile.updateTile()
		}
	}

	private fun setInactive() {
		kotlin.runCatching {
			qsTile.state = Tile.STATE_INACTIVE
			qsTile.updateTile()
		}
	}

	private fun setUnavailable() {
		kotlin.runCatching {
			qsTile.state = Tile.STATE_UNAVAILABLE
			setTileDescription("")
			qsTile.updateTile()
		}
	}

	private fun setTileDescription(description: String) {
		kotlin.runCatching {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
				qsTile.subtitle = description
			}
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
				qsTile.stateDescription = description
			}
			qsTile.updateTile()
		}
	}

	private fun updateTile(tunnelConfig: TunnelConfig?) {
		kotlin.runCatching {
			tunnelConfig?.let {
				setTileDescription(it.name)
				if (it.isActive) return setActive()
				setInactive()
			}
		}
	}
}
