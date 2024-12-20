package com.zaneschepke.wireguardautotunnel.ui

import android.content.Intent
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.data.repository.AppStateRepository
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
import javax.inject.Inject
import kotlin.system.exitProcess

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

	@Inject
	lateinit var appStateRepository: AppStateRepository

	@Inject
	lateinit var tunnelService: TunnelService

	override fun onCreate(savedInstanceState: Bundle?) {
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

			LaunchedEffect(appUiState.autoTunnelActive) {
				requestAutoTunnelTileServiceUpdate()
			}

			with(appUiState.settings) {
				LaunchedEffect(isAutoTunnelEnabled) {
					this@MainActivity.requestAutoTunnelTileServiceUpdate()
				}
			}

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
									composable<Route.Logs> {
										LogsScreen()
									}
									composable<Route.Config> {
										val args = it.toRoute<Route.Config>()
										ConfigScreen(
											appUiState,
											tunnelId = args.id,
											appViewModel = viewModel,
										)
									}
									composable<Route.TunnelOptions> {
										val args = it.toRoute<Route.TunnelOptions>()
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
									composable<Route.KillSwitch> {
										KillSwitchScreen(appUiState, viewModel)
									}
									composable<Route.SplitTunnel> {
										val args = it.toRoute<Route.SplitTunnel>()
										SplitTunnelScreen(appUiState, args.id, viewModel)
									}
									composable<Route.TunnelAutoTunnel> {
										val args = it.toRoute<Route.SplitTunnel>()
										TunnelAutoTunnelScreen(appUiState, args.id)
									}
								}
							}
						}
					}
				}
			}
		}
	}
	override fun onDestroy() {
		super.onDestroy()
		// save battery by not polling stats while app is closed
		tunnelService.cancelActiveTunnelJobs()
	}
}
