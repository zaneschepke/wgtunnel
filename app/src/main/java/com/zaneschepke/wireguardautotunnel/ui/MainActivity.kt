package com.zaneschepke.wireguardautotunnel.ui

import android.content.Intent
import android.graphics.Color.TRANSPARENT
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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.data.repository.AppStateRepository
import com.zaneschepke.wireguardautotunnel.service.shortcut.ShortcutManager
import com.zaneschepke.wireguardautotunnel.service.tunnel.TunnelService
import com.zaneschepke.wireguardautotunnel.ui.common.navigation.BottomNavBar
import com.zaneschepke.wireguardautotunnel.ui.common.navigation.BottomNavItem
import com.zaneschepke.wireguardautotunnel.ui.common.navigation.LocalNavController
import com.zaneschepke.wireguardautotunnel.ui.common.snackbar.CustomSnackBar
import com.zaneschepke.wireguardautotunnel.ui.common.snackbar.SnackbarControllerProvider
import com.zaneschepke.wireguardautotunnel.ui.screens.main.MainScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.pinlock.PinLockScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.scanner.ScannerScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.SettingsScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.appearance.AppearanceScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.appearance.display.DisplayScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.appearance.language.LanguageScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.autotunnel.AutoTunnelScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.autotunnel.advanced.AdvancedScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.disclosure.LocationDisclosureScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.killswitch.KillSwitchScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.support.SupportScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.support.logs.LogsScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.tunneloptions.OptionsScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.tunneloptions.config.ConfigScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.tunneloptions.splittunnel.SplitTunnelScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.tunneloptions.tunnelautotunnel.TunnelAutoTunnelScreen
import com.zaneschepke.wireguardautotunnel.ui.theme.WireguardAutoTunnelTheme
import com.zaneschepke.wireguardautotunnel.util.Constants
import com.zaneschepke.wireguardautotunnel.util.extensions.requestAutoTunnelTileServiceUpdate
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.system.exitProcess

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

	@Inject
	lateinit var appStateRepository: AppStateRepository

	@Inject
	lateinit var tunnelService: TunnelService

	@Inject
	lateinit var shortcutManager: ShortcutManager

	@OptIn(ExperimentalLayoutApi::class)
	override fun onCreate(savedInstanceState: Bundle?) {
		enableEdgeToEdge(
			statusBarStyle = SystemBarStyle.auto(TRANSPARENT, TRANSPARENT),
			navigationBarStyle = SystemBarStyle.auto(TRANSPARENT, TRANSPARENT),
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

			LaunchedEffect(Unit) {
				viewModel.getEmitSplitTunnelApps(this@MainActivity)
			}

			LaunchedEffect(appUiState.autoTunnelActive) {
				requestAutoTunnelTileServiceUpdate()
			}

			with(appUiState.settings) {
				LaunchedEffect(isAutoTunnelEnabled) {
					this@MainActivity.requestAutoTunnelTileServiceUpdate()
				}
				LaunchedEffect(isShortcutsEnabled) {
					if (!isShortcutsEnabled) return@LaunchedEffect shortcutManager.removeShortcuts()
					shortcutManager.addShortcuts()
				}
			}

			CompositionLocalProvider(LocalNavController provides navController) {
				SnackbarControllerProvider { host ->
					WireguardAutoTunnelTheme(theme = appUiState.generalState.theme) {
						Scaffold(
							modifier = Modifier.background(color = MaterialTheme.colorScheme.background),
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
							Box(modifier = Modifier.fillMaxSize().padding(padding)) {
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
											appUiState,
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
										AdvancedScreen(appUiState, viewModel)
									}
									composable<Route.Logs> {
										LogsScreen()
									}
									composable<Route.Config> {
										val args = it.toRoute<Route.Config>()
										val config = appUiState.tunnels.firstOrNull { it.id == args.id }
										ConfigScreen(config, viewModel)
									}
									composable<Route.TunnelOptions> {
										val args = it.toRoute<Route.TunnelOptions>()
										val config = appUiState.tunnels.first { it.id == args.id }
										OptionsScreen(config)
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
										val args = it.toRoute<Route.SplitTunnel>()
										val config = appUiState.tunnels.first { it.id == args.id }
										SplitTunnelScreen(config, viewModel)
									}
									composable<Route.TunnelAutoTunnel> {
										val args = it.toRoute<Route.TunnelOptions>()
										val config = appUiState.tunnels.first { it.id == args.id }
										TunnelAutoTunnelScreen(config, appUiState.settings)
									}
								}
								BackHandler(enabled = true) {
									lifecycleScope.launch {
										if (!navController.popBackStack()) {
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
	}
}
