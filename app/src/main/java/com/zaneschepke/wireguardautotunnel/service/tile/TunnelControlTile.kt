package com.zaneschepke.wireguardautotunnel.service.tile

import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ServiceLifecycleDispatcher
import androidx.lifecycle.lifecycleScope
import com.zaneschepke.wireguardautotunnel.data.domain.TunnelConfig
import com.zaneschepke.wireguardautotunnel.data.repository.AppDataRepository
import com.zaneschepke.wireguardautotunnel.service.foreground.ServiceManager
import com.zaneschepke.wireguardautotunnel.service.tunnel.TunnelState
import com.zaneschepke.wireguardautotunnel.service.tunnel.VpnService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class TunnelControlTile : TileService(), LifecycleOwner {

    @Inject
    lateinit var appDataRepository: AppDataRepository

    @Inject
    lateinit var vpnService: VpnService

    @Inject
    lateinit var serviceManager: ServiceManager

    private val dispatcher = ServiceLifecycleDispatcher(this)

    private var manualStartConfig: TunnelConfig? = null

    override fun onStartListening() {
        super.onStartListening()
        Timber.d("On start listening called")
        lifecycleScope.launch {
            when (vpnService.getState()) {
                TunnelState.UP -> {
                    setActive()
                    setTileDescription(vpnService.name)
                }

                TunnelState.DOWN -> {
                    setInactive()
                    val config = appDataRepository.getStartTunnelConfig()?.also { config ->
                        manualStartConfig = config
                    } ?: appDataRepository.getPrimaryOrFirstTunnel()
                    config?.let {
                        setTileDescription(it.name)
                    } ?: setUnavailable()
                }

                else -> setInactive()
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
                    if (vpnService.getState() == TunnelState.UP) {
                        serviceManager.stopVpnServiceForeground(
                            this@TunnelControlTile,
                            isManualStop = true,
                        )
                    } else {
                        serviceManager.startVpnServiceForeground(
                            this@TunnelControlTile, manualStartConfig?.id, isManualStart = true,
                        )
                    }
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
