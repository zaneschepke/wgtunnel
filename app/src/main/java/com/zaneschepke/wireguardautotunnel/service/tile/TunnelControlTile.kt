package com.zaneschepke.wireguardautotunnel.service.tile

import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.zaneschepke.wireguardautotunnel.data.repository.AppDataRepository
import com.zaneschepke.wireguardautotunnel.module.ApplicationScope
import com.zaneschepke.wireguardautotunnel.service.foreground.ServiceManager
import com.zaneschepke.wireguardautotunnel.service.tunnel.TunnelService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class TunnelControlTile : TileService() {
	@Inject
	lateinit var appDataRepository: AppDataRepository

	@Inject
	lateinit var tunnelService: TunnelService

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
		Timber.d("Start listening called")
		serviceManager.tunnelControlTile.complete(this)
		applicationScope.launch {
			updateTileState()
		}
	}

	fun updateTileState() = applicationScope.launch {
		if (appDataRepository.tunnels.getAll().isEmpty()) return@launch setUnavailable()
		with(tunnelService.vpnState.value) {
			if (status.isUp() && tunnelConfig != null) return@launch updateTile(tunnelConfig.name, true)
		}
		appDataRepository.getStartTunnelConfig()?.let {
			updateTile(it.name, false)
		}
	}

	override fun onClick() {
		super.onClick()
		unlockAndRun {
			applicationScope.launch {
				if (tunnelService.vpnState.value.status.isUp()) return@launch tunnelService.stopTunnel()
				appDataRepository.getStartTunnelConfig()?.let {
					tunnelService.startTunnel(it, true)
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

	/* This works around an annoying unsolved frameworks bug some people are hitting. */
	override fun onBind(intent: Intent): IBinder? {
		var ret: IBinder? = null
		try {
			ret = super.onBind(intent)
		} catch (_: Throwable) {
			Timber.e("Failed to bind to TunnelControlTile")
		}
		return ret
	}

	private fun updateTile(name: String, active: Boolean) {
		kotlin.runCatching {
			setTileDescription(name)
			if (active) return setActive()
			setInactive()
		}.onFailure {
			Timber.e(it)
		}
	}
}
