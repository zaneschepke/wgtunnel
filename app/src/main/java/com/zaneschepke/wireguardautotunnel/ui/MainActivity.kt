package com.zaneschepke.wireguardautotunnel.ui

import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.data.repository.AppStateRepository
import com.zaneschepke.wireguardautotunnel.service.foreground.ServiceManager
import com.zaneschepke.wireguardautotunnel.service.tunnel.TunnelService
import com.zaneschepke.wireguardautotunnel.service.tunnel.TunnelState
import com.zaneschepke.wireguardautotunnel.ui.common.navigation.BottomNavBar
import com.zaneschepke.wireguardautotunnel.ui.common.navigation.BottomNavItem
import com.zaneschepke.wireguardautotunnel.ui.common.navigation.LocalNavController
import com.zaneschepke.wireguardautotunnel.ui.common.navigation.isCurrentRoute
import com.zaneschepke.wireguardautotunnel.ui.common.prompt.CustomSnackBar
import com.zaneschepke.wireguardautotunnel.ui.common.snackbar.SnackbarControllerProvider
import com.zaneschepke.wireguardautotunnel.ui.screens.config.ConfigScreen
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

	private val viewModel by viewModels<AppViewModel>()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge(
			navigationBarStyle = SystemBarStyle.auto(
				lightScrim = Color.Transparent.toArgb(),
				darkScrim = Color.Transparent.toArgb(),
			),
		)

		installSplashScreen().apply {
			setKeepOnScreenCondition {
				!viewModel.isAppReady.value
			}
		}

		setContent {
			val appUiState by viewModel.uiState.collectAsStateWithLifecycle(lifecycle = this.lifecycle)
			val navController = rememberNavController()
			val navBackStackEntry by navController.currentBackStackEntryAsState()

			LaunchedEffect(appUiState.vpnState.status) {
				val context = this@MainActivity
				when (appUiState.vpnState.status) {
					TunnelState.DOWN -> ServiceManager.stopTunnelBackgroundService(context)
					else -> Unit
				}
				context.requestTunnelTileServiceStateUpdate()
			}

			CompositionLocalProvider(LocalNavController provides navController) {
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
							modifier =
							Modifier
								.focusable()
								.focusProperties {
									if (navBackStackEntry?.isCurrentRoute(Route.Lock) == true) {
										Unit
									} else {
										up = focusRequester
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
									startDestination = (if (appUiState.generalState.isPinLockEnabled == true) Route.Lock else Route.Main),
								) {
									composable<Route.Main> {
										MainScreen(
											focusRequester = focusRequester,
											uiState = appUiState,
										)
									}
									composable<Route.Settings> {
										SettingsScreen(
											appViewModel = viewModel,
											uiState = appUiState,
											focusRequester = focusRequester,
										)
									}
									composable<Route.Support> {
										SupportScreen(
											focusRequester = focusRequester,
											appUiState = appUiState,
										)
									}
									composable<Route.Logs> {
										LogsScreen()
									}
									composable<Route.Config> {
										val args = it.toRoute<Route.Config>()
										ConfigScreen(
											focusRequester = focusRequester,
											tunnelId = args.id,
										)
									}
									composable<Route.Option> {
										val args = it.toRoute<Route.Option>()
										OptionsScreen(
											tunnelId = args.id,
											focusRequester = focusRequester,
											appUiState = appUiState,
										)
									}
									composable<Route.Lock> {
										PinLockScreen(
											appViewModel = viewModel,
										)
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
		tunnelService.cancelStatsJob()
	}
}
