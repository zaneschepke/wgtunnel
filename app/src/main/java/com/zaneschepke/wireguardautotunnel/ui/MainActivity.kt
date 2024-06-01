package com.zaneschepke.wireguardautotunnel.ui

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.WireGuardAutoTunnel
import com.zaneschepke.wireguardautotunnel.data.datastore.DataStoreManager
import com.zaneschepke.wireguardautotunnel.data.repository.SettingsRepository
import com.zaneschepke.wireguardautotunnel.service.foreground.ServiceManager
import com.zaneschepke.wireguardautotunnel.ui.common.navigation.BottomNavBar
import com.zaneschepke.wireguardautotunnel.ui.common.prompt.CustomSnackBar
import com.zaneschepke.wireguardautotunnel.ui.screens.config.ConfigScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.main.ConfigType
import com.zaneschepke.wireguardautotunnel.ui.screens.main.MainScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.options.OptionsScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.pinlock.PinLockScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.SettingsScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.support.SupportScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.support.logs.LogsScreen
import com.zaneschepke.wireguardautotunnel.ui.theme.WireguardAutoTunnelTheme
import com.zaneschepke.wireguardautotunnel.util.StringValue
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import xyz.teamgravity.pin_lock_compose.PinManager
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var dataStoreManager: DataStoreManager

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var serviceManager: ServiceManager

    @OptIn(
        ExperimentalPermissionsApi::class,
    )
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge(navigationBarStyle = SystemBarStyle.dark(Color.Transparent.toArgb()))

        // load preferences into memory and init data
        lifecycleScope.launch {
            dataStoreManager.init()
            WireGuardAutoTunnel.requestTunnelTileServiceStateUpdate()
            val settings = settingsRepository.getSettings()
            if (settings.isAutoTunnelEnabled) {
                serviceManager.startWatcherService(application.applicationContext)
            }
        }
        setContent {
            val appViewModel = hiltViewModel<AppViewModel>()
            val appUiState by appViewModel.appUiState.collectAsStateWithLifecycle()
            val navController = rememberNavController()
            val navBackStackEntry by navController.currentBackStackEntryAsState()

            val notificationPermissionState =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                    rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS) else null

            val snackbarHostState = remember { SnackbarHostState() }

            val vpnActivityResultState =
                rememberLauncherForActivityResult(
                    ActivityResultContracts.StartActivityForResult(),
                    onResult = {
                        val accepted = (it.resultCode == RESULT_OK)
                        if (accepted) {
                            appViewModel.onVpnPermissionAccepted()
                        }
                    },
                )

            fun showSnackBarMessage(message: StringValue) {
                lifecycleScope.launch(Dispatchers.Main) {
                    val result =
                        snackbarHostState.showSnackbar(
                            message = message.asString(this@MainActivity),
                            duration = SnackbarDuration.Short,
                        )
                    when (result) {
                        SnackbarResult.ActionPerformed,
                        SnackbarResult.Dismissed -> {
                            snackbarHostState.currentSnackbarData?.dismiss()
                        }
                    }
                }
            }

            LaunchedEffect(appUiState.requestPermissions) {
                if (appUiState.requestPermissions) {
                    appViewModel.permissionsRequested()
                    if (notificationPermissionState != null && !notificationPermissionState.status.isGranted
                    ) {
                        showSnackBarMessage(StringValue.StringResource(R.string.notification_permission_required))
                        return@LaunchedEffect notificationPermissionState.launchPermissionRequest()
                    }
                    if (!appUiState.vpnPermissionAccepted) {
                        return@LaunchedEffect appViewModel.vpnIntent?.let {
                            vpnActivityResultState.launch(
                                it,
                            )
                        }!!
                    }
                }
            }

            WireguardAutoTunnelTheme {
                LaunchedEffect(Unit) {
                    appViewModel.setNotificationPermissionAccepted(
                        notificationPermissionState?.status?.isGranted ?: true,
                    )
                }

                LaunchedEffect(appUiState.snackbarMessageConsumed) {
                    if (!appUiState.snackbarMessageConsumed) {
                        showSnackBarMessage(StringValue.DynamicString(appUiState.snackbarMessage))
                        appViewModel.snackbarMessageConsumed()
                    }
                }

                val focusRequester = remember { FocusRequester() }

                Scaffold(
                    snackbarHost = {
                        SnackbarHost(snackbarHostState) { snackbarData: SnackbarData ->
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
                    //TODO refactor
                    modifier = Modifier
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
                    NavHost(
                        navController,
                        startDestination =
                        //TODO disable pin lock
                        //(if (PinManager.pinExists()) Screen.Lock.route else Screen.Main.route),
                        Screen.Main.route,
                        modifier =
                        Modifier
                            .padding(padding)
                            .fillMaxSize(),
                    ) {
                        composable(
                            Screen.Main.route,
                        ) {
                            MainScreen(
                                focusRequester = focusRequester,
                                appViewModel = appViewModel,
                                navController = navController,
                            )
                        }
                        composable(
                            Screen.Settings.route,
                        ) {
                            SettingsScreen(
                                appViewModel = appViewModel,
                                navController = navController,
                                focusRequester = focusRequester,
                            )
                        }
                        composable(
                            Screen.Support.route,
                        ) {
                            SupportScreen(
                                focusRequester = focusRequester,
                                appViewModel = appViewModel,
                                navController = navController,
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
                            val configType = ConfigType.valueOf(
                                it.arguments?.getString("configType") ?: ConfigType.WIREGUARD.name,
                            )
                            if (!id.isNullOrBlank()) {
                                ConfigScreen(
                                    navController = navController,
                                    tunnelId = id,
                                    appViewModel = appViewModel,
                                    focusRequester = focusRequester,
                                    configType = configType,
                                )
                            }
                        }
                        composable("${Screen.Option.route}/{id}") {
                            val id = it.arguments?.getString("id")
                            if (!id.isNullOrBlank()) {
                                OptionsScreen(
                                    navController = navController,
                                    tunnelId = id,
                                    appViewModel = appViewModel,
                                    focusRequester = focusRequester,
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
