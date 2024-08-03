package com.zaneschepke.wireguardautotunnel.service.tile

import android.os.Build
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
import com.zaneschepke.wireguardautotunnel.service.tunnel.TunnelState
import com.zaneschepke.wireguardautotunnel.util.extensions.startTunnelBackground
import com.zaneschepke.wireguardautotunnel.util.extensions.stopTunnelBackground
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class TunnelControlTile : TileService(), LifecycleOwner {
	@Inject
	lateinit var appDataRepository: AppDataRepository

	@Inject
	lateinit var tunnelService: TunnelService

	@Inject
	@ApplicationScope
	lateinit var applicationScope: CoroutineScope

	private val lifecycleRegistry: LifecycleRegistry = LifecycleRegistry(this)

	private var tileTunnel: TunnelConfig? = null

	override fun onCreate() {
		super.onCreate()
		Timber.d("onCreate for tile service")
		lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

		applicationScope.launch {
			launch {
				tunnelService.vpnState.map { it.tunnelConfig to it.status }.distinctUntilChanged().collect {
					Timber.d("Tunnel state change tile: ${it.first?.name} : ${it.second}")
					updateTile(it)
				}
			}
			launch {
				appDataRepository.tunnels.getTunnelConfigsFlow().takeIf {
					tunnelService.getState() == TunnelState.DOWN
				}?.collect { tunnels ->
					kotlin.runCatching {
						if (tunnels.isEmpty()) setUnavailable() else setInactive()
					}
					val tunnel = TunnelConfig.findDefault(tunnels)
					Timber.d("Updating tile tunnel ${tunnel?.name}")
					tileTunnel = tunnel
					tunnel?.let {
						setTileDescription(it.name)
					}
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
		Timber.d("Clicked")
		unlockAndRun {
			lifecycleScope.launch {
				Timber.d("In scope")
				val context = this@TunnelControlTile
				when (tunnelService.getState()) {
					TunnelState.UP -> tunnelService.vpnState.value.tunnelConfig?.let {
						Timber.d("Calling stop ${it.name}")
						context.stopTunnelBackground(it.id)
					}
					else -> {
						Timber.d("tunnel down")
						(tileTunnel ?: appDataRepository.getPrimaryOrFirstTunnel())?.let {
							Timber.d("Calling start ${it.name}")
							context.startTunnelBackground(it.id)
						}
					}
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
		setTileDescription("")
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

	private fun updateTile(state: Pair<TunnelConfig?, TunnelState>) {
		kotlin.runCatching {
			when (state.second) {
				TunnelState.UP -> setActive()
				TunnelState.DOWN -> setInactive()
				TunnelState.TOGGLE -> setInactive()
			}
			state.first?.let { config ->
				setTileDescription(config.name)
			}
		}
	}

	override val lifecycle: Lifecycle
		get() = lifecycleRegistry
}
