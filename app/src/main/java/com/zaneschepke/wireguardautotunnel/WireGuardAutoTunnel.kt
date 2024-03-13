package com.zaneschepke.wireguardautotunnel

import android.app.Application
import android.content.ComponentName
import android.content.pm.PackageManager
import android.service.quicksettings.TileService
import com.zaneschepke.wireguardautotunnel.service.tile.TunnelControlTile
import com.zaneschepke.wireguardautotunnel.util.ReleaseTree
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class WireGuardAutoTunnel : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
        if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree()) else Timber.plant(ReleaseTree())
    }

    companion object {
        lateinit var instance: WireGuardAutoTunnel
            private set

        fun isRunningOnAndroidTv(): Boolean {
            return instance.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
        }

        fun requestTileServiceStateUpdate() {
            TileService.requestListeningState(
                instance,
                ComponentName(instance, TunnelControlTile::class.java),
            )
        }
    }
}
