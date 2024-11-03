package com.zaneschepke.wireguardautotunnel.ui

import android.content.Context
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.QuestionMark
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.WireGuardAutoTunnel
import com.zaneschepke.wireguardautotunnel.data.datastore.LocaleStorage
import com.zaneschepke.wireguardautotunnel.data.repository.AppStateRepository
import com.zaneschepke.wireguardautotunnel.service.tunnel.TunnelService
import com.zaneschepke.wireguardautotunnel.ui.common.navigation.BottomNavBar
import com.zaneschepke.wireguardautotunnel.ui.common.navigation.BottomNavItem
import com.zaneschepke.wireguardautotunnel.ui.common.navigation.LocalFocusRequester
import com.zaneschepke.wireguardautotunnel.ui.common.navigation.LocalNavController
import com.zaneschepke.wireguardautotunnel.ui.common.snackbar.CustomSnackBar
import com.zaneschepke.wireguardautotunnel.ui.common.snackbar.SnackbarControllerProvider
import com.zaneschepke.wireguardautotunnel.ui.screens.config.ConfigScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.main.MainScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.options.OptionsScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.pinlock.PinLockScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.scanner.ScannerScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.SettingsScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.appearance.AppearanceScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.appearance.display.DisplayScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.appearance.language.LanguageScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.autotunnel.AutoTunnelScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.disclosure.LocationDisclosureScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.support.SupportScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.support.logs.LogsScreen
import com.zaneschepke.wireguardautotunnel.ui.theme.WireguardAutoTunnelTheme
import com.zaneschepke.wireguardautotunnel.util.Constants
import com.zaneschepke.wireguardautotunnel.util.LocaleUtil
import com.zaneschepke.wireguardautotunnel.util.extensions.requestAutoTunnelTileServiceUpdate
import com.zaneschepke.wireguardautotunnel.util.extensions.requestTunnelTileServiceStateUpdate
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

	private val localeStorage: LocaleStorage by lazy {
		(application as WireGuardAutoTunnel).localeStorage
	}

	private lateinit var oldPrefLocaleCode: String

	@Inject
	lateinit var appStateRepository: AppStateRepository

	@Inject
	lateinit var tunnelService: TunnelService

	private val viewModel by viewModels<AppViewModel>()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		installSplashScreen().apply {
			setKeepOnScreenCondition {
				!viewModel.isAppReady.value
			}
		}

		setContent {
			val appUiState by viewModel.uiState.collectAsStateWithLifecycle(lifecycle = this.lifecycle)
			val navController = rememberNavController()
			val rootItemFocusRequester = remember { FocusRequester() }

			LaunchedEffect(appUiState.tunnels) {
				Timber.d("Updating launched")
				requestTunnelTileServiceStateUpdate()
			}

			LaunchedEffect(appUiState.autoTunnelActive) {
				requestAutoTunnelTileServiceUpdate()
			}

			with(appUiState.settings) {
				LaunchedEffect(isAutoTunnelEnabled) {
					this@MainActivity.requestAutoTunnelTileServiceUpdate()
				}
			}

			CompositionLocalProvider(LocalFocusRequester provides rootItemFocusRequester) {
				CompositionLocalProvider(LocalNavController provides navController) {
					SnackbarControllerProvider { host ->
						WireguardAutoTunnelTheme(theme = appUiState.generalState.theme) {
							Scaffold(
								contentWindowInsets = WindowInsets(0.dp),
								snackbarHost = {
									SnackbarHost(host) { snackbarData: SnackbarData ->
										CustomSnackBar(
											snackbarData.visuals.message,
											isRtl = false,
											containerColor =
											MaterialTheme.colorScheme.surfaceColorAtElevation(
												2.dp,
											),
										)
									}
								},
								bottomBar = {
									BottomNavBar(
										navController,
										listOf(
											BottomNavItem(
												name = stringResource(R.string.tunnels),
												route = Route.Main,
												icon = Icons.Rounded.Home,
											),
											BottomNavItem(
												name = stringResource(R.string.settings),
												route = Route.Settings,
												icon = Icons.Rounded.Settings,
											),
											BottomNavItem(
												name = stringResource(R.string.support),
												route = Route.Support,
												icon = Icons.Rounded.QuestionMark,
											),
										),
									)
								},
							) {
								Box(modifier = Modifier.fillMaxSize().padding(it)) {
									NavHost(
										navController,
										enterTransition = { fadeIn(tween(Constants.TRANSITION_ANIMATION_TIME)) },
										exitTransition = { fadeOut(tween(Constants.TRANSITION_ANIMATION_TIME)) },
										startDestination = (if (appUiState.generalState.isPinLockEnabled == true) Route.Lock else Route.Main),
									) {
										composable<Route.Main> {
											MainScreen(
												uiState = appUiState,
											)
										}
										composable<Route.Settings> {
											SettingsScreen(
												appViewModel = viewModel,
												uiState = appUiState,
											)
										}
										composable<Route.LocationDisclosure> {
											LocationDisclosureScreen(viewModel, appUiState)
										}
										composable<Route.AutoTunnel> {
											AutoTunnelScreen(
												appUiState,
											)
										}
										composable<Route.Appearance> {
											AppearanceScreen()
										}
										composable<Route.Language> {
											LanguageScreen(localeStorage)
										}
										composable<Route.Display> {
											DisplayScreen(appUiState)
										}
										composable<Route.Support> {
											SupportScreen()
										}
										composable<Route.Logs> {
											LogsScreen()
										}
										composable<Route.Config> {
											val args = it.toRoute<Route.Config>()
											ConfigScreen(
												tunnelId = args.id,
											)
										}
										composable<Route.Option> {
											val args = it.toRoute<Route.Option>()
											OptionsScreen(
												tunnelId = args.id,
												appUiState = appUiState,
											)
										}
										composable<Route.Lock> {
											PinLockScreen(
												appViewModel = viewModel,
											)
										}
										composable<Route.Scanner> {
											ScannerScreen()
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}

	override fun attachBaseContext(newBase: Context) {
		oldPrefLocaleCode = LocaleStorage(newBase).getPreferredLocale()
		applyOverrideConfiguration(LocaleUtil.getLocalizedConfiguration(oldPrefLocaleCode))
		super.attachBaseContext(newBase)
	}

	override fun onResume() {
		val currentLocaleCode = LocaleStorage(this).getPreferredLocale()
		if (oldPrefLocaleCode != currentLocaleCode) {
			recreate() // locale is changed, restart the activity to update
			oldPrefLocaleCode = currentLocaleCode
		}
		super.onResume()
	}

	override fun onDestroy() {
		super.onDestroy()
		tunnelService.cancelStatsJob()
	}
}
