package com.zaneschepke.wireguardautotunnel.service.tile

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.zaneschepke.wireguardautotunnel.data.repository.AppDataRepository
import com.zaneschepke.wireguardautotunnel.module.ApplicationScope
import com.zaneschepke.wireguardautotunnel.service.foreground.ServiceManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AutoTunnelControlTile : TileService() {
	@Inject
	lateinit var appDataRepository: AppDataRepository

	@Inject
	lateinit var serviceManager: ServiceManager

	@Inject
	@ApplicationScope
	lateinit var applicationScope: CoroutineScope

	override fun onCreate() {
		super.onCreate()
		serviceManager.autoTunnelTile.complete(this)
	}

	override fun onDestroy() {
		super.onDestroy()
		serviceManager.autoTunnelTile = CompletableDeferred()
	}

	override fun onStartListening() {
		super.onStartListening()
		serviceManager.autoTunnelTile.complete(this)
		applicationScope.launch {
			if (appDataRepository.tunnels.getAll().isEmpty()) return@launch setUnavailable()
			updateTileState()
		}
	}

	fun updateTileState() {
		serviceManager.autoTunnelActive.value.let {
			if (it) setActive() else setInactive()
		}
	}

	override fun onClick() {
		super.onClick()
		unlockAndRun {
			applicationScope.launch {
				if (serviceManager.autoTunnelActive.value) {
					serviceManager.stopAutoTunnel()
					setInactive()
				} else {
					serviceManager.startAutoTunnel(true)
					setActive()
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
			qsTile.updateTile()
		}
	}
}
