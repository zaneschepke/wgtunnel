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
import com.zaneschepke.wireguardautotunnel.service.tunnel.TunnelService
import com.zaneschepke.wireguardautotunnel.util.Constants
import com.zaneschepke.wireguardautotunnel.util.extensions.isRunningOnTv
import com.zaneschepke.wireguardautotunnel.util.extensions.requestAutoTunnelTileServiceUpdate
import com.zaneschepke.wireguardautotunnel.util.extensions.requestTunnelTileServiceStateUpdate
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
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

	override fun onCreate(savedInstanceState: Bundle?) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
			val splashScreen = installSplashScreen()
			splashScreen.setKeepOnScreenCondition { true }
		}
		super.onCreate(savedInstanceState)

		applicationScope.launch {
			if (!this@SplashActivity.isRunningOnTv()) localLogCollector.start()
		}

		lifecycleScope.launch {
			repeatOnLifecycle(Lifecycle.State.CREATED) {
				val pinLockEnabled = appStateRepository.isPinLockEnabled()
				if (pinLockEnabled) {
					PinManager.initialize(WireGuardAutoTunnel.instance)
				}
				// TODO eventually make this support multi-tunnel
				Timber.d("Check for active tunnels")
				val settings = appDataRepository.settings.getSettings()
				if (settings.isKernelEnabled) {
					// delay in case state change is underway while app is opened
					delay(Constants.FOCUS_REQUEST_DELAY)
					val activeTunnels = appDataRepository.tunnels.getActive()
					Timber.d("Kernel mode enabled, seeing if we need to start a tunnel")
					activeTunnels.firstOrNull()?.let {
						Timber.d("Trying to start active kernel tunnel: ${it.name}")
						tunnelService.get().startTunnel(it)
					}
				}
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
