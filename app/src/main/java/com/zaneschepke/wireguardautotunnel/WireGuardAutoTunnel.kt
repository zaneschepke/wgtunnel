package com.zaneschepke.wireguardautotunnel

import android.app.Application
import android.content.Context
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import com.zaneschepke.logcatter.LogReader
import com.zaneschepke.wireguardautotunnel.data.datastore.LocaleStorage
import com.zaneschepke.wireguardautotunnel.data.repository.AppStateRepository
import com.zaneschepke.wireguardautotunnel.module.ApplicationScope
import com.zaneschepke.wireguardautotunnel.module.IoDispatcher
import com.zaneschepke.wireguardautotunnel.util.LocaleUtil
import com.zaneschepke.wireguardautotunnel.util.ReleaseTree
import com.zaneschepke.wireguardautotunnel.util.extensions.isRunningOnTv
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class WireGuardAutoTunnel : Application() {

	val localeStorage: LocaleStorage by lazy {
		LocaleStorage(this)
	}

	@Inject
	@ApplicationScope
	lateinit var applicationScope: CoroutineScope

	@Inject
	lateinit var logReader: LogReader

	@Inject
	lateinit var appStateRepository: AppStateRepository

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
		} else {
			Timber.plant(ReleaseTree())
		}
		if (!isRunningOnTv()) {
			applicationScope.launch(ioDispatcher) {
				if (appStateRepository.isLocalLogsEnabled()) {
					Timber.d("Starting logger")
					logReader.start()
				}
			}
		}
	}

	override fun attachBaseContext(base: Context) {
		super.attachBaseContext(LocaleUtil.getLocalizedContext(base, LocaleStorage(base).getPreferredLocale()))
	}

	companion object {
		lateinit var instance: WireGuardAutoTunnel
			private set
	}
}
