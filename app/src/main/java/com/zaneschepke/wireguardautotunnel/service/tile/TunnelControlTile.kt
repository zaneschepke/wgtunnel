package com.zaneschepke.wireguardautotunnel.service.tile

import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.lifecycleScope
import com.zaneschepke.wireguardautotunnel.data.domain.TunnelConfig
import com.zaneschepke.wireguardautotunnel.data.repository.AppDataRepository
import com.zaneschepke.wireguardautotunnel.module.ApplicationScope
import com.zaneschepke.wireguardautotunnel.service.tunnel.TunnelService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Provider

@AndroidEntryPoint
class TunnelControlTile : TileService(), LifecycleOwner {
	@Inject
	lateinit var appDataRepository: AppDataRepository

	@Inject
	lateinit var tunnelService: Provider<TunnelService>

	@Inject
	@ApplicationScope
	lateinit var applicationScope: CoroutineScope

	private val lifecycleRegistry: LifecycleRegistry = LifecycleRegistry(this)

	override fun onCreate() {
		super.onCreate()
		lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
	}

	override fun onStopListening() {
		lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
	}

	override fun onDestroy() {
		super.onDestroy()
		lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
	}

	override fun onStartListening() {
		super.onStartListening()
		lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
		Timber.d("Updating tile!")
		lifecycleScope.launch {
			if (appDataRepository.tunnels.getAll().isEmpty()) return@launch setUnavailable()
			updateTileState()
		}
	}

	private suspend fun updateTileState() {
		val lastActive = appDataRepository.getStartTunnelConfig()
		lastActive?.let {
			updateTile(it)
		}
	}

	override fun onClick() {
		super.onClick()
		unlockAndRun {
			lifecycleScope.launch {
				val lastActive = appDataRepository.getStartTunnelConfig()
				lastActive?.let { tunnel ->
					if (tunnel.isActive) {
						tunnelService.get().stopTunnel(tunnel)
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

	override val lifecycle: Lifecycle
		get() = lifecycleRegistry
}
