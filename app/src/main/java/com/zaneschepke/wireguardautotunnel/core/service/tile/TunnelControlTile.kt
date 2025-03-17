package com.zaneschepke.wireguardautotunnel.core.service.tile

import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.lifecycleScope
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.core.service.ServiceManager
import com.zaneschepke.wireguardautotunnel.core.tunnel.TunnelManager
import com.zaneschepke.wireguardautotunnel.domain.repository.AppDataRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class TunnelControlTile : TileService(), LifecycleOwner {
	@Inject
	lateinit var appDataRepository: AppDataRepository

	@Inject
	lateinit var serviceManager: ServiceManager

	@Inject
	lateinit var tunnelManager: TunnelManager

	private val lifecycleRegistry: LifecycleRegistry = LifecycleRegistry(this)

	private var isCollecting = false

	override fun onCreate() {
		super.onCreate()
		lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
	}

	override fun onDestroy() {
		super.onDestroy()
		lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
	}

	override fun onStartListening() {
		super.onStartListening()
		lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
		Timber.d("Start listening called for tunnel tile")
		if (isCollecting) return
		isCollecting = true
		lifecycleScope.launch {
			tunnelManager.activeTunnels.collect {
				updateTileState()
			}
		}
	}

	private fun updateTileState() = lifecycleScope.launch {
		val tunnels = appDataRepository.tunnels.getAll()
		if (tunnels.isEmpty()) return@launch setUnavailable()
		with(tunnelManager.activeTunnels.value) {
			if (isNotEmpty()) if (size == 1) {
				tunnels.firstOrNull { it.id == keys.first().id }?.let { return@launch updateTile(it.tunName, true) }
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
			lifecycleScope.launch {
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
	override val lifecycle: Lifecycle
		get() = lifecycleRegistry
}
