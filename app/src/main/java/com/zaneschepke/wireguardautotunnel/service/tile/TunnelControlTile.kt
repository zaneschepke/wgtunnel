package com.zaneschepke.wireguardautotunnel.service.tile

import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.zaneschepke.wireguardautotunnel.data.domain.TunnelConfig
import com.zaneschepke.wireguardautotunnel.data.repository.AppDataRepository
import com.zaneschepke.wireguardautotunnel.service.foreground.ServiceManager
import com.zaneschepke.wireguardautotunnel.service.tunnel.TunnelState
import com.zaneschepke.wireguardautotunnel.service.tunnel.VpnService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class TunnelControlTile : TileService() {

    @Inject
    lateinit var appDataRepository: AppDataRepository

    @Inject
    lateinit var vpnService: VpnService

    @Inject
    lateinit var serviceManager: ServiceManager

    private val scope = CoroutineScope(Dispatchers.IO)

    private var manualStartConfig: TunnelConfig? = null

    override fun onStartListening() {
        super.onStartListening()
        Timber.d("On start listening called")
        scope.launch {
            vpnService.vpnState.collect { it ->
                when (it.status) {
                    TunnelState.UP -> {
                        setActive()
                        it.tunnelConfig?.name?.let { name -> setTileDescription(name) }
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
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    override fun onTileRemoved() {
        super.onTileRemoved()
        scope.cancel()
    }

    override fun onTileAdded() {
        super.onTileAdded()
        onStartListening()
    }

    override fun onClick() {
        super.onClick()
        unlockAndRun {
            scope.launch {
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
}
