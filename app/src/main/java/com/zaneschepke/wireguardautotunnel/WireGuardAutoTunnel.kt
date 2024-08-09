package com.zaneschepke.wireguardautotunnel

import android.app.Application
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import com.zaneschepke.wireguardautotunnel.data.repository.AppDataRepository
import com.zaneschepke.wireguardautotunnel.module.ApplicationScope
import com.zaneschepke.wireguardautotunnel.service.tunnel.TunnelService
import com.zaneschepke.wireguardautotunnel.util.ReleaseTree
import com.zaneschepke.wireguardautotunnel.util.extensions.requestAutoTunnelTileServiceUpdate
import com.zaneschepke.wireguardautotunnel.util.extensions.requestTunnelTileServiceStateUpdate
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Provider

@HiltAndroidApp
class WireGuardAutoTunnel : Application() {

	@Inject
	lateinit var tunnelService: Provider<TunnelService>

	@Inject
	lateinit var appDataRepository: AppDataRepository

	@Inject
	@ApplicationScope
	lateinit var applicationScope: CoroutineScope

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
		applicationScope.launch {
			// TODO eventually make this support multitunnel
			Timber.d("Check for active tunnels")
			val activeTunnels = appDataRepository.tunnels.getActive()
			val settings = appDataRepository.settings.getSettings()
			if (settings.isKernelEnabled) {
				Timber.d("Kernel mode enabled, seeing if we need to start a tunnel")
				activeTunnels.firstOrNull()?.let {
					Timber.d("Trying to start active kernel tunnel: ${it.name}")
					tunnelService.get().startTunnel(it)
				}
			}
		}
		requestTunnelTileServiceStateUpdate()
		requestAutoTunnelTileServiceUpdate()
	}

	companion object {
		lateinit var instance: WireGuardAutoTunnel
			private set
	}
}
