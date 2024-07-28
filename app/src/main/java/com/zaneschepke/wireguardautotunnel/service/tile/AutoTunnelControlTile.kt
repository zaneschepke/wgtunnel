package com.zaneschepke.wireguardautotunnel.service.tile

import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ServiceLifecycleDispatcher
import androidx.lifecycle.lifecycleScope
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.data.domain.TunnelConfig
import com.zaneschepke.wireguardautotunnel.data.repository.AppDataRepository
import com.zaneschepke.wireguardautotunnel.service.foreground.ServiceManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class AutoTunnelControlTile : TileService(), LifecycleOwner {
	@Inject
	lateinit var appDataRepository: AppDataRepository

	@Inject
	lateinit var serviceManager: ServiceManager

	private val dispatcher = ServiceLifecycleDispatcher(this)

	private var manualStartConfig: TunnelConfig? = null

	override fun onStartListening() {
		super.onStartListening()
		lifecycleScope.launch {
			val settings = appDataRepository.settings.getSettings()
			when (settings.isAutoTunnelEnabled) {
				true -> {
					if (settings.isAutoTunnelPaused) {
						setInactive()
						setTileDescription(this@AutoTunnelControlTile.getString(R.string.paused))
					} else {
						setActive()
						setTileDescription(this@AutoTunnelControlTile.getString(R.string.active))
					}
				}

				false -> {
					setTileDescription(this@AutoTunnelControlTile.getString(R.string.disabled))
					setUnavailable()
				}
			}
		}
	}

	override fun onTileAdded() {
		super.onTileAdded()
		onStartListening()
	}

	override fun onClick() {
		super.onClick()
		unlockAndRun {
			lifecycleScope.launch {
				try {
					appDataRepository.toggleWatcherServicePause()
					onStartListening()
				} catch (e: Exception) {
					Timber.e(e.message)
				} finally {
					cancel()
				}
			}
		}
	}

	private fun setActive() {
		qsTile.state = Tile.STATE_ACTIVE
		qsTile.updateTile()
	}

	private fun setInactive() {
		qsTile.state = Tile.STATE_INACTIVE
		qsTile.updateTile()
	}

	private fun setUnavailable() {
		manualStartConfig = null
		qsTile.state = Tile.STATE_UNAVAILABLE
		qsTile.updateTile()
	}

	private fun setTileDescription(description: String) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
			qsTile.subtitle = description
		}
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			qsTile.stateDescription = description
		}
		qsTile.updateTile()
	}

	override val lifecycle: Lifecycle
		get() = dispatcher.lifecycle
}
