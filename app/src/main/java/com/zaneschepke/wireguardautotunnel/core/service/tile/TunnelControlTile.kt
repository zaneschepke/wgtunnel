package com.zaneschepke.wireguardautotunnel.core.service.tile

import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.core.service.ServiceManager
import com.zaneschepke.wireguardautotunnel.core.tunnel.TunnelManager
import com.zaneschepke.wireguardautotunnel.di.ApplicationScope
import com.zaneschepke.wireguardautotunnel.domain.repository.AppDataRepository
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
	@ApplicationScope
	lateinit var applicationScope: CoroutineScope

	@Inject
	lateinit var serviceManager: ServiceManager

	@Inject
	lateinit var tunnelManager: TunnelManager

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
		val tunnels = appDataRepository.tunnels.getAll()
		if (tunnels.isEmpty()) return@launch setUnavailable()
		with(tunnelManager.activeTunnels.value) {
			if (isNotEmpty()) if (size == 1) {
				tunnels.firstOrNull { it.id == keys.first() }?.let { return@launch updateTile(it.tunName, true) }
			} else {
				return@launch updateTile(getString(R.string.multiple), true)
			}
		}
		appDataRepository.getStartTunnelConfig()?.let {
			updateTile(it.tunName, false)
		}
	}

	override fun onClick() {
		super.onClick()
		unlockAndRun {
			applicationScope.launch {
				if (tunnelManager.activeTunnels.value.isNotEmpty()) return@launch tunnelManager.stopTunnel()
				appDataRepository.getStartTunnelConfig()?.let {
					tunnelManager.startTunnel(it)
				}
			}
		}
	}

	private fun setActive() {
		runCatching {
			qsTile.state = Tile.STATE_ACTIVE
			qsTile.updateTile()
		}
	}

	private fun setInactive() {
		runCatching {
			qsTile.state = Tile.STATE_INACTIVE
			qsTile.updateTile()
		}
	}

	private fun setUnavailable() {
		runCatching {
			qsTile.state = Tile.STATE_UNAVAILABLE
			setTileDescription("")
			qsTile.updateTile()
		}
	}

	private fun setTileDescription(description: String) {
		runCatching {
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
		runCatching {
			setTileDescription(name)
			if (active) return setActive()
			setInactive()
		}.onFailure {
			Timber.e(it)
		}
	}
}
