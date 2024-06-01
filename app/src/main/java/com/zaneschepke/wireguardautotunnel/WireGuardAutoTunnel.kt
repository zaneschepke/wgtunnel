package com.zaneschepke.wireguardautotunnel

import android.app.Application
import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.service.quicksettings.TileService
import com.zaneschepke.logcatter.LocalLogCollector
import com.zaneschepke.wireguardautotunnel.module.ApplicationScope
import com.zaneschepke.wireguardautotunnel.module.IoDispatcher
import com.zaneschepke.wireguardautotunnel.service.tile.AutoTunnelControlTile
import com.zaneschepke.wireguardautotunnel.service.tile.TunnelControlTile
import com.zaneschepke.wireguardautotunnel.util.ReleaseTree
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import xyz.teamgravity.pin_lock_compose.PinManager
import javax.inject.Inject

@HiltAndroidApp
class WireGuardAutoTunnel : Application() {

    @Inject
    lateinit var localLogCollector: LocalLogCollector

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    @Inject
    @IoDispatcher
    lateinit var ioDispatcher: CoroutineDispatcher

    override fun onCreate() {
        super.onCreate()
        instance = this
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
            StrictMode.setThreadPolicy(
                ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()
                    .penaltyLog()
                    .build(),
            )
        } else Timber.plant(ReleaseTree())
        applicationScope.launch(ioDispatcher) {
            //TODO disable pin lock for now
            //PinManager.initialize(this@WireGuardAutoTunnel)
            if (!isRunningOnAndroidTv()) localLogCollector.start()
        }
    }

    companion object {

        lateinit var instance: WireGuardAutoTunnel
            private set

        fun isRunningOnAndroidTv(): Boolean {
            return instance.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
        }

        fun requestTunnelTileServiceStateUpdate() {
            TileService.requestListeningState(
                instance,
                ComponentName(instance, TunnelControlTile::class.java),
            )
        }

        fun requestAutoTunnelTileServiceUpdate() {
            TileService.requestListeningState(
                instance,
                ComponentName(instance, AutoTunnelControlTile::class.java),
            )
        }
    }
}
