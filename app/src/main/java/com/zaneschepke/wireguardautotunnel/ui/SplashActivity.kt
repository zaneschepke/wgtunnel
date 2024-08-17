package com.zaneschepke.wireguardautotunnel.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.zaneschepke.logcatter.LocalLogCollector
import com.zaneschepke.wireguardautotunnel.WireGuardAutoTunnel
import com.zaneschepke.wireguardautotunnel.data.repository.AppDataRepository
import com.zaneschepke.wireguardautotunnel.data.repository.AppStateRepository
import com.zaneschepke.wireguardautotunnel.module.ApplicationScope
import com.zaneschepke.wireguardautotunnel.module.IoDispatcher
import com.zaneschepke.wireguardautotunnel.service.foreground.ServiceManager
import com.zaneschepke.wireguardautotunnel.service.tunnel.TunnelService
import com.zaneschepke.wireguardautotunnel.service.tunnel.TunnelState
import com.zaneschepke.wireguardautotunnel.util.extensions.isRunningOnTv
import com.zaneschepke.wireguardautotunnel.util.extensions.requestAutoTunnelTileServiceUpdate
import com.zaneschepke.wireguardautotunnel.util.extensions.requestTunnelTileServiceStateUpdate
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import xyz.teamgravity.pin_lock_compose.PinManager
import javax.inject.Inject
import javax.inject.Provider

@SuppressLint("CustomSplashScreen")
@AndroidEntryPoint
class SplashActivity : ComponentActivity() {
	@Inject
	lateinit var appStateRepository: AppStateRepository

	@Inject
	lateinit var appDataRepository: AppDataRepository

	@Inject
	lateinit var tunnelService: Provider<TunnelService>

	@Inject
	lateinit var localLogCollector: LocalLogCollector

	@Inject
	@ApplicationScope
	lateinit var applicationScope: CoroutineScope

	@Inject
	@IoDispatcher
	lateinit var ioDispatcher: CoroutineDispatcher

	@Inject
	lateinit var serviceManager: ServiceManager

	override fun onCreate(savedInstanceState: Bundle?) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
			val splashScreen = installSplashScreen()
			splashScreen.setKeepOnScreenCondition { true }
		}
		super.onCreate(savedInstanceState)

		applicationScope.launch(ioDispatcher) {
			if (!this@SplashActivity.isRunningOnTv()) localLogCollector.start()
		}

		lifecycleScope.launch(ioDispatcher) {
			repeatOnLifecycle(Lifecycle.State.CREATED) {
				val pinLockEnabled = appStateRepository.isPinLockEnabled()
				if (pinLockEnabled) {
					PinManager.initialize(WireGuardAutoTunnel.instance)
				}
				val settings = appDataRepository.settings.getSettings()
				if (settings.isAutoTunnelEnabled) serviceManager.startWatcherService(application.applicationContext)
				if (tunnelService.get().getState() == TunnelState.UP) tunnelService.get().startStatsJob()
				val tunnels = appDataRepository.tunnels.getActive()
				if (tunnels.isNotEmpty() && tunnelService.get().getState() == TunnelState.DOWN) tunnelService.get().startTunnel(tunnels.first())
				requestTunnelTileServiceStateUpdate()
				requestAutoTunnelTileServiceUpdate()

				val intent =
					Intent(this@SplashActivity, MainActivity::class.java).apply {
						putExtra(IS_PIN_LOCK_ENABLED_KEY, pinLockEnabled)
					}
				startActivity(intent)
				finish()
			}
		}
	}

	companion object {
		const val IS_PIN_LOCK_ENABLED_KEY = "is_pin_lock_enabled"
	}
}
