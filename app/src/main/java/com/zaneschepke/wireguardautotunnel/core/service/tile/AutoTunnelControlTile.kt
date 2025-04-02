package com.zaneschepke.wireguardautotunnel.core.service.tile

import android.content.Intent
import android.os.IBinder
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.lifecycleScope
import com.zaneschepke.wireguardautotunnel.core.service.ServiceManager
import com.zaneschepke.wireguardautotunnel.domain.repository.AppDataRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class AutoTunnelControlTile : TileService(), LifecycleOwner {
	@Inject
	lateinit var appDataRepository: AppDataRepository

	@Inject
	lateinit var serviceManager: ServiceManager

	private val lifecycleRegistry: LifecycleRegistry = LifecycleRegistry(this)

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
		Timber.d("Start listening called for auto tunnel tile")
		lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
		lifecycleScope.launch {
			serviceManager.autoTunnelActive.collect {
				if (it) setActive() else setInactive()
			}
		}
		lifecycleScope.launch {
			appDataRepository.tunnels.flow.collect {
				if (it.isEmpty()) {
					setUnavailable()
				} else {
					if (qsTile.state == Tile.STATE_ACTIVE) setInactive()
				}
			}
		}
	}

	override fun onClick() {
		super.onClick()
		unlockAndRun {
			lifecycleScope.launch {
				if (serviceManager.autoTunnelActive.value) {
					serviceManager.stopAutoTunnel()
					setInactive()
				} else {
					serviceManager.startAutoTunnel()
					setActive()
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

	private fun setUnavailable() {
		runCatching {
			qsTile.state = Tile.STATE_UNAVAILABLE
			qsTile.updateTile()
		}
	}

	override val lifecycle: Lifecycle
		get() = lifecycleRegistry
}
