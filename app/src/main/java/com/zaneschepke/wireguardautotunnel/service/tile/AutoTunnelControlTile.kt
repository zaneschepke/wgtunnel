package com.zaneschepke.wireguardautotunnel.service.tile

import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.lifecycleScope
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.data.repository.AppDataRepository
import com.zaneschepke.wireguardautotunnel.module.ApplicationScope
import com.zaneschepke.wireguardautotunnel.service.foreground.ServiceManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class AutoTunnelControlTile : TileService(), LifecycleOwner {
	@Inject
	lateinit var appDataRepository: AppDataRepository

	@Inject
	lateinit var serviceManager: ServiceManager

	@Inject
	@ApplicationScope
	lateinit var applicationScope: CoroutineScope

	private val lifecycleRegistry: LifecycleRegistry = LifecycleRegistry(this)

	override fun onCreate() {
		super.onCreate()
		lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

		applicationScope.launch {
			appDataRepository.settings.getSettingsFlow().collect {
				kotlin.runCatching {
					when (it.isAutoTunnelEnabled) {
						true -> {
							if (it.isAutoTunnelPaused) {
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
				}.onFailure {
					Timber.e(it)
				}
			}
		}
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
	}

	override fun onClick() {
		super.onClick()
		unlockAndRun {
			lifecycleScope.launch {
				kotlin.runCatching {
					val settings = appDataRepository.settings.getSettings()
					if (settings.isAutoTunnelPaused) {
						return@launch appDataRepository.settings.save(
							settings.copy(
								isAutoTunnelPaused = false,
							),
						)
					}
					appDataRepository.settings.save(
						settings.copy(
							isAutoTunnelPaused = true,
						),
					)
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
		get() = lifecycleRegistry
}
