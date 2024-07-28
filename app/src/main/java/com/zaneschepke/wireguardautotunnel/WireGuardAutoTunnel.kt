package com.zaneschepke.wireguardautotunnel

import android.app.Application
import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.service.quicksettings.TileService
import com.zaneschepke.wireguardautotunnel.service.tile.AutoTunnelControlTile
import com.zaneschepke.wireguardautotunnel.service.tile.TunnelControlTile
import com.zaneschepke.wireguardautotunnel.util.ReleaseTree
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class WireGuardAutoTunnel : Application() {
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
		} else {
			Timber.plant(ReleaseTree())
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
