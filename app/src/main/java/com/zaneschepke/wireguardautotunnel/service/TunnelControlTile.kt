package com.zaneschepke.wireguardautotunnel.service

import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.wireguard.android.backend.Tunnel
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.repository.Repository
import com.zaneschepke.wireguardautotunnel.service.foreground.Action
import com.zaneschepke.wireguardautotunnel.service.foreground.ServiceTracker
import com.zaneschepke.wireguardautotunnel.service.foreground.WireGuardConnectivityWatcherService
import com.zaneschepke.wireguardautotunnel.service.foreground.WireGuardTunnelService
import com.zaneschepke.wireguardautotunnel.service.tunnel.VpnService
import com.zaneschepke.wireguardautotunnel.service.tunnel.model.Settings
import com.zaneschepke.wireguardautotunnel.service.tunnel.model.TunnelConfig
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
    lateinit var settingsRepo : Repository<Settings>

    @Inject
    lateinit var configRepo : Repository<TunnelConfig>

    @Inject
    lateinit var vpnService : VpnService

    private val scope = CoroutineScope(Dispatchers.Main);

    private lateinit var job : Job

    override fun onStartListening() {
        if (!this::job.isInitialized) {
            job = scope.launch {
                updateTileState()
            }
        }
        Timber.d("On start listening")
        super.onStartListening()
    }

    override fun onTileAdded() {
        super.onTileAdded()
        qsTile.contentDescription = "Toggle VPN"
        scope.launch {
            updateTileState();
        }
    }

    override fun onTileRemoved() {
        super.onTileRemoved()
        cancelJob()
    }

    override fun onClick() {
        unlockAndRun {
            scope.launch {
                try {
                    if(vpnService.getState() == Tunnel.State.UP) {
                        stopTunnel();
                        return@launch
                    }
                    val settings = settingsRepo.getAll()
                    if (!settings.isNullOrEmpty()) {
                        val setting = settings.first()
                        if (setting.defaultTunnel != null) {
                            startTunnel(setting.defaultTunnel!!)
                        } else {
                            val config = configRepo.getAll()?.first();
                            if(config != null) {
                                startTunnel(config.toString());
                            }
                        }
                    }
                } finally {
                    cancel()
                }
            }
            super.onClick()
        }
    }

    private fun stopTunnel() {
        ServiceTracker.actionOnService(
            Action.STOP, this@TunnelControlTile,
            WireGuardConnectivityWatcherService::class.java)
        ServiceTracker.actionOnService(
            Action.STOP, this@TunnelControlTile,
            WireGuardTunnelService::class.java)
    }

    private fun startTunnel(tunnelConfig : String) {
        ServiceTracker.actionOnService(
            Action.START, this.applicationContext,
            WireGuardTunnelService::class.java,
            mapOf(this.applicationContext.resources.
            getString(R.string.tunnel_extras_key) to
                    tunnelConfig))
    }

    private suspend fun updateTileState() {
        vpnService.state.collect {
            when(it) {
                Tunnel.State.UP -> {
                    setTileOn()
                }
                Tunnel.State.DOWN -> {
                    setTileOff()
                }
                else -> {
                    qsTile.state = Tile.STATE_UNAVAILABLE
                }
            }
            qsTile.updateTile()
        }
    }

    private fun setTileOff() {
        qsTile.state = Tile.STATE_INACTIVE;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            qsTile.subtitle = "Off"
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            qsTile.stateDescription = "VPN Off";
        }
    }

    private fun setTileOn() {
        qsTile.state = Tile.STATE_ACTIVE;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            qsTile.subtitle = "On"
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            qsTile.stateDescription = "VPN On";
        }
    }
    private fun cancelJob() {
        if(this::job.isInitialized) {
            job.cancel();
        }
    }
}