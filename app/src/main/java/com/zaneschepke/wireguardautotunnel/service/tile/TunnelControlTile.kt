package com.zaneschepke.wireguardautotunnel.service.tile

import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.wireguard.android.backend.Tunnel
import com.zaneschepke.wireguardautotunnel.data.model.TunnelConfig
import com.zaneschepke.wireguardautotunnel.data.repository.SettingsRepository
import com.zaneschepke.wireguardautotunnel.data.repository.TunnelConfigRepository
import com.zaneschepke.wireguardautotunnel.service.foreground.ServiceManager
import com.zaneschepke.wireguardautotunnel.service.tunnel.VpnService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class TunnelControlTile() : TileService() {

    @Inject lateinit var tunnelConfigRepository: TunnelConfigRepository

    @Inject lateinit var settingsRepository: SettingsRepository

    @Inject lateinit var vpnService: VpnService

    private val scope = CoroutineScope(Dispatchers.IO)

    private var tunnelName: String? = null

    override fun onStartListening() {
        super.onStartListening()
        Timber.d("On start listening called")
        scope.launch {
            vpnService.vpnState.collect {
                when (it.status) {
                    Tunnel.State.UP -> setActive()
                    Tunnel.State.DOWN -> setInactive()
                    else -> setInactive()
                }
                val tunnels = tunnelConfigRepository.getAll()
                if (tunnels.isEmpty()) {
                    setUnavailable()
                    return@collect
                }
                tunnelName = it.name.run {
                        val settings = settingsRepository.getSettings()
                        if (settings.defaultTunnel != null) {
                            TunnelConfig.from(settings.defaultTunnel!!).name
                        } else tunnels.firstOrNull()?.name
                    }
                setTileDescription(tunnelName ?: "")
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

    override fun onClick() {
        super.onClick()
        unlockAndRun {
            scope.launch {
                try {
                    val defaultTunnel = settingsRepository.getSettings().defaultTunnel
                    val config = defaultTunnel ?: run {
                        val tunnelConfigs = tunnelConfigRepository.getAll()
                        return@run tunnelConfigs.find { it.name == tunnelName }
                    }
                    toggleWatcherServicePause()
                    if (vpnService.getState() == Tunnel.State.UP) {
                        ServiceManager.stopVpnService(this@TunnelControlTile)
                    } else {
                        ServiceManager.startVpnServiceForeground(
                            this@TunnelControlTile,
                            config.toString(),
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

    private fun toggleWatcherServicePause() {
        scope.launch {
            val settings = settingsRepository.getSettings()
            if (settings.isAutoTunnelEnabled) {
                val pauseAutoTunnel = !settings.isAutoTunnelPaused
                settingsRepository.save(
                    settings.copy(
                        isAutoTunnelPaused = pauseAutoTunnel,
                    ),
                )
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
}
