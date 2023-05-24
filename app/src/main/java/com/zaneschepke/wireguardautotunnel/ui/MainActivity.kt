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
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.wireguard.android.backend.GoBackend
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.common.PermissionRequestFailedScreen
import com.zaneschepke.wireguardautotunnel.ui.common.navigation.BottomNavBar
import com.zaneschepke.wireguardautotunnel.ui.screens.main.MainScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.SettingsScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.support.SupportScreen
import com.zaneschepke.wireguardautotunnel.ui.theme.TransparentSystemBars
import com.zaneschepke.wireguardautotunnel.ui.theme.WireguardAutoTunnelTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class,
        ExperimentalPermissionsApi::class
    )
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberAnimatedNavController()
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
                val vpnActivityResultState = rememberLauncherForActivityResult(
                    ActivityResultContracts.StartActivityForResult(),
                    onResult = {
                        val accepted = (it.resultCode == RESULT_OK)
                        if (accepted) {
                            vpnIntent = null
                        }
                    })
                LaunchedEffect(vpnIntent) {
                    if (vpnIntent != null) {
                        vpnActivityResultState.launch(vpnIntent)
                    } else requestNotificationPermission()
                }

                Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) },
                    bottomBar = if (vpnIntent == null && notificationPermissionState.status.isGranted) {
                        { BottomNavBar(navController, Routes.navItems) }
                    } else {
                        {}
                    },
                )
                { padding ->
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
                                    Uri.fromParts("package", this.packageName, null)
                                startActivity(intentSettings);
                            },
                            message = getString(R.string.notification_permission_required),
                            getString(R.string.open_settings)
                        )
                        return@Scaffold
                    }
                    AnimatedNavHost(navController, startDestination = Routes.Main.name) {
                        composable(Routes.Main.name, enterTransition = {
                            when (initialState.destination.route) {
                                Routes.Settings.name, Routes.Support.name ->
                                    slideInHorizontally(
                                        initialOffsetX = { -1000 },
                                        animationSpec = tween(500)
                                    )

                                else -> {
                                    fadeIn(animationSpec = tween(2000))
                                }
                            }
                        }) {
                            MainScreen(padding = padding, snackbarHostState = snackbarHostState)
                        }
                        composable(Routes.Settings.name, enterTransition = {
                            when (initialState.destination.route) {
                                Routes.Main.name ->
                                    slideInHorizontally(
                                        initialOffsetX = { 1000 },
                                        animationSpec = tween(500)
                                    )

                                Routes.Support.name -> {
                                    slideInHorizontally(
                                        initialOffsetX = { -1000 },
                                        animationSpec = tween(500)
                                    )
                                }

                                else -> {
                                    fadeIn(animationSpec = tween(2000))
                                }
                            }
                        }) { SettingsScreen(padding = padding, snackbarHostState = snackbarHostState, navController = navController) }
                        composable(Routes.Support.name, enterTransition = {
                            when (initialState.destination.route) {
                                Routes.Settings.name, Routes.Main.name ->
                                    slideInHorizontally(
                                        initialOffsetX = { 1000 },
                                        animationSpec = tween(500)
                                    )

                                else -> {
                                    fadeIn(animationSpec = tween(2000))
                                }
                            }
                        }) { SupportScreen(padding = padding) }
                    }
                }
            }
        }
    }
}
