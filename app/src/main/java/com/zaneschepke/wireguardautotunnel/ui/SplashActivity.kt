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
import com.zaneschepke.wireguardautotunnel.WireGuardAutoTunnel.Companion.isRunningOnAndroidTv
import com.zaneschepke.wireguardautotunnel.data.repository.AppStateRepository
import com.zaneschepke.wireguardautotunnel.module.ApplicationScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import xyz.teamgravity.pin_lock_compose.PinManager
import javax.inject.Inject

@SuppressLint("CustomSplashScreen")
@AndroidEntryPoint
class SplashActivity : ComponentActivity() {
	@Inject
	lateinit var appStateRepository: AppStateRepository

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
			if (!isRunningOnAndroidTv()) localLogCollector.start()
		}

		lifecycleScope.launch {
			repeatOnLifecycle(Lifecycle.State.CREATED) {
				val pinLockEnabled = appStateRepository.isPinLockEnabled()
				if (pinLockEnabled) {
					PinManager.initialize(WireGuardAutoTunnel.instance)
				}

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
