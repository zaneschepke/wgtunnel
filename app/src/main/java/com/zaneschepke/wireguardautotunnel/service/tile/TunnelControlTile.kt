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
		lifecycleScope.launch {
			val settings = appDataRepository.settings.getSettings()
			if (appDataRepository.tunnels.getAll().isEmpty()) return@launch setUnavailable()
			if (settings.isKernelEnabled) return@launch updateTileStateKernel()
			updateTileStateUserspace()
		}
	}

	private suspend fun updateTileStateUserspace() {
		val vpnState = tunnelService.vpnState.value
		when (vpnState.status) {
			TunnelState.UP -> updateTile(vpnState.tunnelConfig?.copy(isActive = true))
			else -> {
				val tunnel = appDataRepository.getStartTunnelConfig()
				updateTile(tunnel?.copy(isActive = false))
			}
		}
	}

	private suspend fun updateTileStateKernel() {
		val lastActive = appDataRepository.appState.getActiveTunnelId()
		lastActive?.let {
			val tunnel = appDataRepository.tunnels.getById(it)
			updateTile(tunnel)
		}
	}

	override fun onClick() {
		super.onClick()
		unlockAndRun {
			lifecycleScope.launch {
				val context = this@TunnelControlTile
				val lastActive = appDataRepository.appState.getActiveTunnelId()
				lastActive?.let { lastTun ->
					val tunnel = appDataRepository.tunnels.getById(lastTun)
					tunnel?.let { tun ->
						if (tun.isActive) return@launch context.stopTunnelBackground(tun.id)
						context.startTunnelBackground(tun.id)
					}
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

	override val lifecycle: Lifecycle
		get() = lifecycleRegistry
}
