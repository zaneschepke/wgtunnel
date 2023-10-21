package com.zaneschepke.wireguardautotunnel.service.tile

import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.wireguard.android.backend.Tunnel
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.repository.SettingsDoa
import com.zaneschepke.wireguardautotunnel.repository.TunnelConfigDao
import com.zaneschepke.wireguardautotunnel.repository.model.TunnelConfig
import com.zaneschepke.wireguardautotunnel.service.foreground.ServiceManager
import com.zaneschepke.wireguardautotunnel.service.tunnel.VpnService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class TunnelControlTile : TileService() {

    @Inject
    lateinit var settingsRepo : SettingsDoa

    @Inject
    lateinit var configRepo : TunnelConfigDao

    @Inject
    lateinit var vpnService : VpnService

    private val scope = CoroutineScope(Dispatchers.Main)

    private lateinit var job : Job

    override fun onStartListening() {
        job = scope.launch {
            updateTileState()
        }
        super.onStartListening()
    }

    override fun onTileAdded() {
        super.onTileAdded()
        qsTile.contentDescription = this.resources.getString(R.string.toggle_vpn)
        scope.launch {
            updateTileState()
        }
    }

    override fun onTileRemoved() {
        super.onTileRemoved()
        cancelJob()
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    override fun onClick() {
        super.onClick()
        unlockAndRun {
            scope.launch {
                try {
                    val tunnel = determineTileTunnel()
                    if(tunnel != null) {
                        attemptWatcherServiceToggle(tunnel.toString())
                        if(vpnService.getState() == Tunnel.State.UP) {
                            ServiceManager.stopVpnService(this@TunnelControlTile)
                        } else {
                            ServiceManager.startVpnServiceForeground(this@TunnelControlTile, tunnel.toString())
                        }
                    }
                } catch (e : Exception) {
                    Timber.e(e.message)
                } finally {
                    cancel()
                }
            }
        }
    }

    private suspend fun determineTileTunnel() : TunnelConfig? {
        var tunnelConfig : TunnelConfig? = null
        val settings = settingsRepo.getAll()
        if (settings.isNotEmpty()) {
            val setting = settings.first()
            tunnelConfig = if (setting.defaultTunnel != null) {
                TunnelConfig.from(setting.defaultTunnel!!)
            } else {
                val configs = configRepo.getAll()
                val config  = if(configs.isNotEmpty()) {
                    configs.first()
                } else {
                    null
                }
                config
            }
        }
        return tunnelConfig
    }


    private fun attemptWatcherServiceToggle(tunnelConfig : String) {
        scope.launch {
            val settings = settingsRepo.getAll()
            if (settings.isNotEmpty()) {
                val setting = settings.first()
                if(setting.isAutoTunnelEnabled) {
                    ServiceManager.toggleWatcherServiceForeground(this@TunnelControlTile, tunnelConfig)
                }
            }
        }
    }

    private suspend fun updateTileState() {
        vpnService.state.collect {
            when(it) {
                Tunnel.State.UP -> {
                    qsTile.state = Tile.STATE_ACTIVE
                }
                Tunnel.State.DOWN -> {
                    qsTile.state = Tile.STATE_INACTIVE
                }
                else -> {
                    qsTile.state = Tile.STATE_UNAVAILABLE
                }
            }
            val config = determineTileTunnel()
            setTileDescription(config?.name ?: this.resources.getString(R.string.no_tunnel_available))
            qsTile.updateTile()
        }
    }

    private fun setTileDescription(description : String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            qsTile.subtitle = description
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            qsTile.stateDescription = description
        }
    }

    private fun cancelJob() {
        if(this::job.isInitialized) {
            job.cancel()
        }
    }
}