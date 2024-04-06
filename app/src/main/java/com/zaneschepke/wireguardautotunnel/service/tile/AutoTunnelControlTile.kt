package com.zaneschepke.wireguardautotunnel.service.tile

import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.data.model.TunnelConfig
import com.zaneschepke.wireguardautotunnel.data.repository.AppDataRepository
import com.zaneschepke.wireguardautotunnel.service.foreground.ServiceManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class AutoTunnelControlTile : TileService() {

    @Inject
    lateinit var appDataRepository: AppDataRepository

    @Inject
    lateinit var serviceManager: ServiceManager

    private val scope = CoroutineScope(Dispatchers.IO)

    private var manualStartConfig: TunnelConfig? = null

    override fun onStartListening() {
        super.onStartListening()
        scope.launch {
            appDataRepository.settings.getSettingsFlow().collectLatest {
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
            }
        }
    }

    override fun onTileAdded() {
        super.onTileAdded()
        onStartListening()
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
                    appDataRepository.toggleWatcherServicePause()
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
