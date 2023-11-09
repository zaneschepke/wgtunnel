package com.zaneschepke.wireguardautotunnel.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
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
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.wireguard.android.backend.GoBackend
import com.zaneschepke.wireguardautotunnel.Constants
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.common.PermissionRequestFailedScreen
import com.zaneschepke.wireguardautotunnel.ui.common.navigation.BottomNavBar
import com.zaneschepke.wireguardautotunnel.ui.common.prompt.CustomSnackBar
import com.zaneschepke.wireguardautotunnel.ui.screens.config.ConfigScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.detail.DetailScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.main.MainScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.SettingsScreen
import com.zaneschepke.wireguardautotunnel.ui.screens.support.SupportScreen
import com.zaneschepke.wireguardautotunnel.ui.theme.TransparentSystemBars
import com.zaneschepke.wireguardautotunnel.ui.theme.WireguardAutoTunnelTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @OptIn(ExperimentalAnimationApi::class,
        ExperimentalPermissionsApi::class
    )
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()
            val focusRequester = remember { FocusRequester() }

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

                fun showSnackBarMessage(message : String) {
                    lifecycleScope.launch(Dispatchers.Main) {
                        val result = snackbarHostState.showSnackbar(
                            message = message,
                            actionLabel = applicationContext.getString(R.string.okay),
                            duration = SnackbarDuration.Short,
                        )
                        when (result) {
                            SnackbarResult.ActionPerformed -> { snackbarHostState.currentSnackbarData?.dismiss() }
                            SnackbarResult.Dismissed -> { snackbarHostState.currentSnackbarData?.dismiss() }
                        }
                    }
                }

                Scaffold(snackbarHost = {
                        SnackbarHost(snackbarHostState) { snackbarData: SnackbarData ->
                            CustomSnackBar(
                                snackbarData.visuals.message,
                                isRtl = false,
                                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
                            )
                        }
                    },
                    modifier = Modifier.onKeyEvent {
                        if (it.nativeKeyEvent.action == KeyEvent.ACTION_UP) {
                            when (it.nativeKeyEvent.keyCode) {
                                KeyEvent.KEYCODE_DPAD_UP -> {
                                    try {
                                        focusRequester.requestFocus()
                                    } catch(e : IllegalStateException) {
                                        Timber.e("No D-Pad focus request modifier added to element on screen")
                                    }
                                    false
                                } else -> {
                                   false
                                }
                            }
                        } else {
                            false
                        }
                    },
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
                                    Uri.fromParts(Constants.URI_PACKAGE_SCHEME, this.packageName, null)
                                startActivity(intentSettings)
                            },
                            message = getString(R.string.notification_permission_required),
                            getString(R.string.open_settings)
                        )
                        return@Scaffold
                    }

                    NavHost(navController, startDestination = Routes.Main.name) {
                        composable(Routes.Main.name, enterTransition = {
                            when (initialState.destination.route) {
                                Routes.Settings.name, Routes.Support.name ->
                                    slideInHorizontally(
                                        initialOffsetX = { -Constants.SLIDE_IN_TRANSITION_OFFSET },
                                        animationSpec = tween(Constants.SLIDE_IN_ANIMATION_DURATION)
                                    )

                                else -> {
                                    fadeIn(animationSpec = tween(Constants.FADE_IN_ANIMATION_DURATION))
                                }
                            }
                        }, exitTransition = {
                            ExitTransition.None
                        }
                            ) {
                            MainScreen(padding = padding, showSnackbarMessage = { message -> showSnackBarMessage(message) }, navController = navController)
                        }
                        composable(Routes.Settings.name, enterTransition = {
                            when (initialState.destination.route) {
                                Routes.Main.name ->
                                    slideInHorizontally(
                                        initialOffsetX = { Constants.SLIDE_IN_TRANSITION_OFFSET },
                                        animationSpec = tween(Constants.SLIDE_IN_ANIMATION_DURATION)
                                    )

                                Routes.Support.name -> {
                                    slideInHorizontally(
                                        initialOffsetX = { -Constants.SLIDE_IN_TRANSITION_OFFSET },
                                        animationSpec = tween(Constants.SLIDE_IN_ANIMATION_DURATION)
                                    )
                                }

                                else -> {
                                    fadeIn(animationSpec = tween(Constants.FADE_IN_ANIMATION_DURATION))
                                }
                            }
                        }) { SettingsScreen(padding = padding, showSnackbarMessage = { message -> showSnackBarMessage(message) }, focusRequester = focusRequester) }
                        composable(Routes.Support.name, enterTransition = {
                            when (initialState.destination.route) {
                                Routes.Settings.name, Routes.Main.name ->
                                    slideInHorizontally(
                                        initialOffsetX = { Constants.SLIDE_IN_ANIMATION_DURATION },
                                        animationSpec = tween(Constants.SLIDE_IN_ANIMATION_DURATION)
                                    )

                                else -> {
                                    fadeIn(animationSpec = tween(Constants.FADE_IN_ANIMATION_DURATION))
                                }
                            }
                        }) { SupportScreen(padding = padding, focusRequester) }
                        composable("${Routes.Config.name}/{id}", enterTransition = {
                            fadeIn(animationSpec = tween(Constants.FADE_IN_ANIMATION_DURATION))
                        }) { it ->
                            val id = it.arguments?.getString("id")
                            if(!id.isNullOrBlank()) {
                                ConfigScreen(navController = navController, id = id, showSnackbarMessage = { message -> showSnackBarMessage(message) }, focusRequester = focusRequester)}
                            }
                        composable("${Routes.Detail.name}/{id}", enterTransition = {
                            fadeIn(animationSpec = tween(Constants.FADE_IN_ANIMATION_DURATION))
                        }) {
                            val id = it.arguments?.getString("id")
                            if(!id.isNullOrBlank()) {
                                DetailScreen(padding = padding, focusRequester = focusRequester, id = id)
                            }
                        }
                    }
                }
            }
        }
    }
}
