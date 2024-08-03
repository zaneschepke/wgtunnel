package com.zaneschepke.wireguardautotunnel

import android.app.Application
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
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
	}
}
