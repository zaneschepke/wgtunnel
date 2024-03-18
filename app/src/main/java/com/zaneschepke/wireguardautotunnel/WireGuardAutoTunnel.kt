package com.zaneschepke.wireguardautotunnel

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.service.quicksettings.TileService
import com.zaneschepke.wireguardautotunnel.service.tile.TunnelControlTile
import com.zaneschepke.wireguardautotunnel.util.ReleaseTree
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import xyz.teamgravity.pin_lock_compose.PinManager

@HiltAndroidApp
class WireGuardAutoTunnel : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
        if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree()) else Timber.plant(ReleaseTree())
        PinManager.initialize(this)
    }
    companion object {
        lateinit var instance: WireGuardAutoTunnel
            private set

        fun isRunningOnAndroidTv(): Boolean {
            return instance.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
        }

        fun requestTileServiceStateUpdate(context : Context) {
            TileService.requestListeningState(
                context,
                ComponentName(instance, TunnelControlTile::class.java),
            )
        }
    }
}
