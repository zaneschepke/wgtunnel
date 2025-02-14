package com.zaneschepke.wireguardautotunnel

import android.app.Application
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.wireguard.android.backend.GoBackend
import com.zaneschepke.logcatter.LogReader
import com.zaneschepke.wireguardautotunnel.core.tunnel.TunnelManager
import com.zaneschepke.wireguardautotunnel.di.ApplicationScope
import com.zaneschepke.wireguardautotunnel.di.IoDispatcher
import com.zaneschepke.wireguardautotunnel.di.MainDispatcher
import com.zaneschepke.wireguardautotunnel.domain.enums.BackendState
import com.zaneschepke.wireguardautotunnel.domain.repository.AppDataRepository
import com.zaneschepke.wireguardautotunnel.util.LocaleUtil
import com.zaneschepke.wireguardautotunnel.util.ReleaseTree
import com.zaneschepke.wireguardautotunnel.util.extensions.isRunningOnTv
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class WireGuardAutoTunnel : Application() {

	@Inject
	@ApplicationScope
	lateinit var applicationScope: CoroutineScope

	@Inject
	lateinit var logReader: LogReader

	@Inject
	lateinit var appDataRepository: AppDataRepository

	@Inject
	@IoDispatcher
	lateinit var ioDispatcher: CoroutineDispatcher

	@Inject
	@MainDispatcher
	lateinit var mainDispatcher: CoroutineDispatcher

	@Inject
	lateinit var tunnelManager: TunnelManager

	override fun onCreate() {
		super.onCreate()
		instance = this
		ProcessLifecycleOwner.get().lifecycle.addObserver(AppLifecycleObserver())
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

		GoBackend.setAlwaysOnCallback {
			applicationScope.launch {
				val settings = appDataRepository.settings.get()
				if (settings.isAlwaysOnVpnEnabled) {
					val tunnel = appDataRepository.getPrimaryOrFirstTunnel()
					tunnel?.let {
						tunnelManager.startTunnel(it)
					}
				} else {
					Timber.Forest.w("Always-on VPN is not enabled in app settings")
				}
			}
		}

		applicationScope.launch {
			withContext(mainDispatcher) {
				if (appDataRepository.appState.isLocalLogsEnabled() && !isRunningOnTv()) logReader.initialize()
			}
			if (!appDataRepository.settings.get().isKernelEnabled) {
				tunnelManager.setBackendState(BackendState.SERVICE_ACTIVE, emptyList())
			}
			appDataRepository.appState.getLocale()?.let {
				withContext(mainDispatcher) {
					LocaleUtil.changeLocale(it)
				}
			}
		}
	}

	override fun onTerminate() {
		applicationScope.launch {
			tunnelManager.setBackendState(BackendState.SERVICE_ACTIVE, emptyList())
		}
		super.onTerminate()
	}

	class AppLifecycleObserver : DefaultLifecycleObserver {

		override fun onStart(owner: LifecycleOwner) {
			Timber.d("Application entered foreground")
			foreground = true
		}
		override fun onPause(owner: LifecycleOwner) {
			Timber.d("Application entered background")
			foreground = false
		}
	}

	companion object {
		private var foreground = false

		fun isForeground(): Boolean {
			return foreground
		}

		lateinit var instance: WireGuardAutoTunnel
			private set
	}
}
