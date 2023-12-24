package com.zaneschepke.wireguardautotunnel.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.focusable
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.wireguard.android.backend.GoBackend
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.common.PermissionRequestFailedScreen
import com.zaneschepke.wireguardautotunnel.ui.common.navigation.BottomNavBar
import com.zaneschepke.wireguardautotunnel.ui.common.prompt.CustomSnackBar
import com.zaneschepke.wireguardautotunnel.ui.screens.config.ConfigScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.main.MainScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.SettingsScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.support.SupportScreen
import com.zaneschepke.wireguardautotunnel.ui.theme.TransparentSystemBars
import com.zaneschepke.wireguardautotunnel.ui.theme.WireguardAutoTunnelTheme
import com.zaneschepke.wireguardautotunnel.util.Constants
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @OptIn(
        ExperimentalPermissionsApi::class
    )
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
//            val activityViewModel = hiltViewModel<ActivityViewModel>()

            val navController = rememberNavController()
            val focusRequester = remember { FocusRequester()}

            WireguardAutoTunnelTheme {
                TransparentSystemBars()

                val snackbarHostState = remember { SnackbarHostState() }

                val notificationPermissionState =
                    rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)

                fun requestNotificationPermission() {
                    if (!notificationPermissionState.status.isGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionState.launchPermissionRequest()
                    }
                }

                var vpnIntent by remember { mutableStateOf(GoBackend.VpnService.prepare(this)) }
                val vpnActivityResultState =
                    rememberLauncherForActivityResult(
                        ActivityResultContracts.StartActivityForResult(),
                        onResult = {
                            val accepted = (it.resultCode == RESULT_OK)
                            if (accepted) {
                                vpnIntent = null
                            }
                        }
                    )
                LaunchedEffect(vpnIntent) {
                    if (vpnIntent != null) {
                        vpnActivityResultState.launch(vpnIntent)
                    } else {
                        requestNotificationPermission()
                    }
                }

                fun showSnackBarMessage(message: String) {
                    lifecycleScope.launch(Dispatchers.Main) {
                        val result = snackbarHostState.showSnackbar(
                                message = message,
                                actionLabel = applicationContext.getString(R.string.okay),
                                duration = SnackbarDuration.Short
                            )
                        when (result) {
                            SnackbarResult.ActionPerformed, SnackbarResult.Dismissed -> {
                                snackbarHostState.currentSnackbarData?.dismiss()
                            }
                        }
                    }
                }

                Scaffold(
                    snackbarHost = {
                        SnackbarHost(snackbarHostState) { snackbarData: SnackbarData ->
                            CustomSnackBar(
                                snackbarData.visuals.message,
                                isRtl = false,
                                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(
                                    2.dp
                                )
                            )
                        }
                    },
                    modifier = Modifier.focusable().focusProperties { up = focusRequester },
                    bottomBar =
                    if (vpnIntent == null && notificationPermissionState.status.isGranted) {
                        { BottomNavBar(navController, listOf(
                            Screen.Main.navItem,
                            Screen.Settings.navItem,
                            Screen.Support.navItem)) }
                    } else {
                        {}
                    }
                ) { padding ->
                    if (vpnIntent != null) {
                        PermissionRequestFailedScreen(
                            padding = padding,
                            onRequestAgain = { vpnActivityResultState.launch(vpnIntent) },
                            message = getString(R.string.vpn_permission_required),
                            getString(R.string.retry)
                        )
                        return@Scaffold
                    }
                    if (!notificationPermissionState.status.isGranted) {
                        PermissionRequestFailedScreen(
                            padding = padding,
                            onRequestAgain = {
                                val intentSettings =
                                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                intentSettings.data =
                                    Uri.fromParts(
                                        Constants.URI_PACKAGE_SCHEME,
                                        this.packageName,
                                        null
                                    )
                                startActivity(intentSettings)
                            },
                            message = getString(R.string.notification_permission_required),
                            getString(R.string.open_settings)
                        )
                        return@Scaffold
                    }
                    NavHost(navController, startDestination = Screen.Main.route) {
                        composable(
                            Screen.Main.route,
                        ) {
                            MainScreen(padding = padding, focusRequester = focusRequester, showSnackbarMessage = { message ->
                                showSnackBarMessage(message)
                            }, navController = navController)
                        }
                        composable(Screen.Settings.route,
                        ) {
                            SettingsScreen(padding = padding, showSnackbarMessage = { message ->
                                showSnackBarMessage(message)
                            }, focusRequester = focusRequester)
                        }
                        composable(Screen.Support.route,
                        ) {
                            SupportScreen(padding = padding, focusRequester = focusRequester,
                                showSnackbarMessage = { message ->
                                    showSnackBarMessage(message)
                                })
                        }
                        composable("${Screen.Config.route}/{id}") {
                            val id = it.arguments?.getString("id")
                            if (!id.isNullOrBlank()) {
                                //https://dagger.dev/hilt/view-model#assisted-injection
                                ConfigScreen(
                                    navController = navController,
                                    id = id,
                                    showSnackbarMessage = { message ->
                                        showSnackBarMessage(message)
                                    },
                                    focusRequester = focusRequester
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
