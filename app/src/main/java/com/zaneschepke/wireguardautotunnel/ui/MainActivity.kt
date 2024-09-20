package com.zaneschepke.wireguardautotunnel.ui

import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Surface
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.zaneschepke.wireguardautotunnel.data.repository.AppStateRepository
import com.zaneschepke.wireguardautotunnel.service.foreground.ServiceManager
import com.zaneschepke.wireguardautotunnel.service.tunnel.TunnelService
import com.zaneschepke.wireguardautotunnel.service.tunnel.TunnelState
import com.zaneschepke.wireguardautotunnel.ui.common.navigation.BottomNavBar
import com.zaneschepke.wireguardautotunnel.ui.common.prompt.CustomSnackBar
import com.zaneschepke.wireguardautotunnel.ui.common.snackbar.SnackbarControllerProvider
import com.zaneschepke.wireguardautotunnel.ui.screens.config.ConfigScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.config.ConfigViewModel
import com.zaneschepke.wireguardautotunnel.ui.screens.main.ConfigType
import com.zaneschepke.wireguardautotunnel.ui.screens.main.MainScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.options.OptionsScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.pinlock.PinLockScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.SettingsScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.support.SupportScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.support.logs.LogsScreen
import com.zaneschepke.wireguardautotunnel.ui.theme.WireguardAutoTunnelTheme
import com.zaneschepke.wireguardautotunnel.util.Constants
import com.zaneschepke.wireguardautotunnel.util.extensions.requestTunnelTileServiceStateUpdate
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
	@Inject
	lateinit var appStateRepository: AppStateRepository

	@Inject
	lateinit var tunnelService: TunnelService

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		val isPinLockEnabled = intent.extras?.getBoolean(SplashActivity.IS_PIN_LOCK_ENABLED_KEY)

		enableEdgeToEdge(
			navigationBarStyle = SystemBarStyle.auto(
				lightScrim = Color.Transparent.toArgb(),
				darkScrim = Color.Transparent.toArgb(),
			),
		)

		setContent {
			val appViewModel = hiltViewModel<AppViewModel>()
			val appUiState by appViewModel.uiState.collectAsStateWithLifecycle(lifecycle = this.lifecycle)
			val navController = appViewModel.navHostController
			val navBackStackEntry by navController.currentBackStackEntryAsState()

			LaunchedEffect(appUiState.vpnState.status) {
				val context = this@MainActivity
				when (appUiState.vpnState.status) {
					TunnelState.DOWN -> ServiceManager.stopTunnelBackgroundService(context)
					else -> Unit
				}
				context.requestTunnelTileServiceStateUpdate()
			}

			SnackbarControllerProvider { host ->
				WireguardAutoTunnelTheme {
					val focusRequester = remember { FocusRequester() }
					Scaffold(
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
						containerColor = MaterialTheme.colorScheme.background,
						modifier =
						Modifier
							.focusable()
							.focusProperties {
								when (navBackStackEntry?.destination?.route) {
									Screen.Lock.route -> Unit
									else -> up = focusRequester
								}
							},
						bottomBar = {
							BottomNavBar(
								navController,
								listOf(
									Screen.Main.navItem,
									Screen.Settings.navItem,
									Screen.Support.navItem,
								),
							)
						},
					) { padding ->
						Surface(modifier = Modifier.fillMaxSize().padding(padding)) {
							NavHost(
								navController,
								enterTransition = { fadeIn(tween(Constants.TRANSITION_ANIMATION_TIME)) },
								exitTransition = { fadeOut(tween(Constants.TRANSITION_ANIMATION_TIME)) },
								startDestination = (if (isPinLockEnabled == true) Screen.Lock.route else Screen.Main.route),
							) {
								composable(
									Screen.Main.route,
								) {
									MainScreen(
										focusRequester = focusRequester,
										uiState = appUiState,
										navController = navController,
									)
								}
								composable(
									Screen.Settings.route,
								) {
									SettingsScreen(
										appViewModel = appViewModel,
										uiState = appUiState,
										navController = navController,
										focusRequester = focusRequester,
									)
								}
								composable(
									Screen.Support.route,
								) {
									SupportScreen(
										focusRequester = focusRequester,
										navController = navController,
										appUiState = appUiState,
									)
								}
								composable(Screen.Support.Logs.route) {
									LogsScreen()
								}
								composable(
									"${Screen.Config.route}/{id}?configType={configType}",
									arguments =
									listOf(
										navArgument("id") {
											type = NavType.StringType
											defaultValue = "0"
										},
										navArgument("configType") {
											type = NavType.StringType
											defaultValue = ConfigType.WIREGUARD.name
										},
									),
								) {
									val id = it.arguments?.getString("id")
									if (!id.isNullOrBlank()) {
										val viewModel = hiltViewModel<ConfigViewModel, ConfigViewModel.ConfigViewModelFactory> { factory ->
											factory.create(id.toInt())
										}
										ConfigScreen(
											viewModel = viewModel,
											focusRequester = focusRequester,
											tunnelId = id.toInt(),
										)
									}
								}
								composable("${Screen.Option.route}/{id}") {
									val id = it.arguments?.getString("id")
									if (!id.isNullOrBlank()) {
										OptionsScreen(
											navController = navController,
											tunnelId = id.toInt(),
											focusRequester = focusRequester,
											appUiState = appUiState,
										)
									}
								}
								composable(Screen.Lock.route) {
									PinLockScreen(
										navController = navController,
										appViewModel = appViewModel,
									)
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
		tunnelService.cancelStatsJob()
	}
}
