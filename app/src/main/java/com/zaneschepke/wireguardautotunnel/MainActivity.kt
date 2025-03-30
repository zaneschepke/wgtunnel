package com.zaneschepke.wireguardautotunnel

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.zaneschepke.networkmonitor.NetworkMonitor
import com.zaneschepke.wireguardautotunnel.core.shortcut.ShortcutManager
import com.zaneschepke.wireguardautotunnel.core.tunnel.TunnelManager
import com.zaneschepke.wireguardautotunnel.domain.repository.AppStateRepository
import com.zaneschepke.wireguardautotunnel.ui.Route
import com.zaneschepke.wireguardautotunnel.ui.common.navigation.BottomNavBar
import com.zaneschepke.wireguardautotunnel.ui.common.navigation.BottomNavItem
import com.zaneschepke.wireguardautotunnel.ui.common.navigation.LocalNavController
import com.zaneschepke.wireguardautotunnel.ui.common.snackbar.CustomSnackBar
import com.zaneschepke.wireguardautotunnel.ui.common.snackbar.SnackbarControllerProvider
import com.zaneschepke.wireguardautotunnel.ui.screens.main.config.ConfigScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.main.MainScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.main.OptionsScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.main.PinLockScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.main.ScannerScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.main.splittunnel.SplitTunnelScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.main.TunnelAutoTunnelScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.AdvancedScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.AppearanceScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.DisplayScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.KillSwitchScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.LanguageScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.LocationDisclosureScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.SettingsScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.autotunnel.AutoTunnelScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.support.LogsScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.support.SupportScreen
import com.zaneschepke.wireguardautotunnel.ui.theme.WireguardAutoTunnelTheme
import com.zaneschepke.wireguardautotunnel.util.Constants
import com.zaneschepke.wireguardautotunnel.viewmodel.AppViewModel
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject
import kotlin.system.exitProcess

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

	@Inject
	lateinit var appStateRepository: AppStateRepository

	@Inject
	lateinit var tunnelManager: TunnelManager

	@Inject
	lateinit var shortcutManager: ShortcutManager

	@Inject
	lateinit var networkMonitor: NetworkMonitor

	private var lastLocationPermissionState: Boolean? = null

	override fun onCreate(savedInstanceState: Bundle?) {
		enableEdgeToEdge(
			statusBarStyle = SystemBarStyle.Companion.auto(Color.TRANSPARENT, Color.TRANSPARENT),
			navigationBarStyle = SystemBarStyle.Companion.auto(Color.TRANSPARENT, Color.TRANSPARENT),
		)
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
			window.isNavigationBarContrastEnforced = false
		}
		super.onCreate(savedInstanceState)

		val viewModel by viewModels<AppViewModel>()

		installSplashScreen().apply {
			setKeepOnScreenCondition {
				!viewModel.isAppReady.value
			}
		}

		setContent {
			val appUiState by viewModel.uiState.collectAsStateWithLifecycle()
			val configurationChange by viewModel.configurationChange.collectAsStateWithLifecycle()
			val navController = rememberNavController()

			LaunchedEffect(configurationChange) {
				if (configurationChange) {
					Intent(this@MainActivity, MainActivity::class.java).also {
						startActivity(it)
						exitProcess(0)
					}
				}
			}

			with(appUiState.appSettings) {
				LaunchedEffect(isShortcutsEnabled) {
					if (!isShortcutsEnabled) return@LaunchedEffect shortcutManager.removeShortcuts()
					shortcutManager.addShortcuts()
				}
			}

			CompositionLocalProvider(LocalNavController provides navController) {
				SnackbarControllerProvider { host ->
					WireguardAutoTunnelTheme(theme = appUiState.generalState.theme) {
						Scaffold(
							contentWindowInsets = WindowInsets(0),
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
						) { padding ->
							Box(modifier = Modifier.Companion.fillMaxSize().padding(padding)) {
								NavHost(
									navController,
									enterTransition = { fadeIn(tween(Constants.TRANSITION_ANIMATION_TIME)) },
									exitTransition = { fadeOut(tween(Constants.TRANSITION_ANIMATION_TIME)) },
									startDestination = (if (appUiState.generalState.isPinLockEnabled) Route.Lock else Route.Main),
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
											appUiState.appSettings,
										)
									}
									composable<Route.Appearance> {
										AppearanceScreen()
									}
									composable<Route.Language> {
										LanguageScreen(appUiState, viewModel)
									}
									composable<Route.Display> {
										DisplayScreen(appUiState)
									}
									composable<Route.Support> {
										SupportScreen(appUiState, viewModel)
									}
									composable<Route.AutoTunnelAdvanced> {
										AdvancedScreen(appUiState.appSettings, viewModel)
									}
									composable<Route.Logs> {
										LogsScreen()
									}
									composable<Route.Config> { backStack ->
										val args = backStack.toRoute<Route.Config>()
										val config =
											appUiState.tunnels.firstOrNull { it.id == args.id }
										ConfigScreen(config)
									}
									composable<Route.TunnelOptions> { backStack ->
										val args = backStack.toRoute<Route.TunnelOptions>()
										appUiState.tunnels.firstOrNull { it.id == args.id }?.let { config ->
											OptionsScreen(config, appUiState)
										}
									}
									composable<Route.Lock> {
										PinLockScreen(viewModel)
									}
									composable<Route.Scanner> {
										ScannerScreen()
									}
									composable<Route.KillSwitch> {
										KillSwitchScreen(appUiState, viewModel)
									}
									composable<Route.SplitTunnel> {
										SplitTunnelScreen()
									}
									composable<Route.TunnelAutoTunnel> { backStack ->
										val args = backStack.toRoute<Route.TunnelOptions>()
										appUiState.tunnels.firstOrNull { it.id == args.id }?.let {
											TunnelAutoTunnelScreen(it, appUiState.appSettings)
										}
									}
								}
								BackHandler {
									if (navController.previousBackStackEntry == null || !navController.popBackStack()) {
										this@MainActivity.finish()
									}
								}
							}
						}
					}
				}
			}
		}
	}
	override fun onResume() {
		super.onResume()
		checkPermissionAndNotify()
	}

	private fun checkPermissionAndNotify() {
		val hasLocation = ContextCompat.checkSelfPermission(
			this,
			Manifest.permission.ACCESS_FINE_LOCATION,
		) == PackageManager.PERMISSION_GRANTED
		if (lastLocationPermissionState != hasLocation) {
			Timber.d("Location permission changed to: $hasLocation")
			if (hasLocation) {
				networkMonitor.sendLocationPermissionsGrantedBroadcast()
			}
			lastLocationPermissionState = hasLocation
		}
	}
}
