package com.zaneschepke.wireguardautotunnel.ui.screens.autotunnel

import android.Manifest
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.zaneschepke.wireguardautotunnel.ui.Route
import com.zaneschepke.wireguardautotunnel.ui.common.SectionDivider
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SurfaceSelectionGroupButton
import com.zaneschepke.wireguardautotunnel.ui.navigation.LocalIsAndroidTV
import com.zaneschepke.wireguardautotunnel.ui.navigation.LocalNavController
import com.zaneschepke.wireguardautotunnel.ui.screens.autotunnel.components.AdvancedSettingsItem
import com.zaneschepke.wireguardautotunnel.ui.screens.autotunnel.components.NetworkTunnelingItems
import com.zaneschepke.wireguardautotunnel.ui.screens.autotunnel.components.WifiTunnelingItems
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.components.BackgroundLocationDialog
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.components.LocationServicesDialog
import com.zaneschepke.wireguardautotunnel.ui.state.AppUiState
import com.zaneschepke.wireguardautotunnel.util.extensions.isLocationServicesEnabled
import com.zaneschepke.wireguardautotunnel.viewmodel.AppViewModel

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun AutoTunnelScreen(uiState: AppUiState, viewModel: AppViewModel) {
    val context = LocalContext.current
    val navController = LocalNavController.current
    val isTv = LocalIsAndroidTV.current
    val fineLocationState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    var currentText by remember { mutableStateOf("") }
    var isBackgroundLocationGranted by remember { mutableStateOf(true) }
    var showLocationServicesAlertDialog by remember { mutableStateOf(false) }
    var showLocationDialog by remember { mutableStateOf(false) }

    fun checkFineLocationGranted() {
        isBackgroundLocationGranted = fineLocationState.status.isGranted
    }

    fun isWifiNameReadable(): Boolean {
        return when {
            !isBackgroundLocationGranted || !fineLocationState.status.isGranted -> {
                showLocationDialog = true
                false
            }
            !context.isLocationServicesEnabled() -> {
                showLocationServicesAlertDialog = true
                false
            }
            else -> true
        }
    }

    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) checkFineLocationGranted()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        if (isTv && Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
            checkFineLocationGranted()
        } else {
            val backgroundLocationState =
                rememberPermissionState(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
    }

    LaunchedEffect(uiState.appSettings.trustedNetworkSSIDs) { currentText = "" }

    LocationServicesDialog(
        showLocationServicesAlertDialog,
        onDismiss = { showLocationServicesAlertDialog = false },
        onAttest = { showLocationServicesAlertDialog = false },
    )

    BackgroundLocationDialog(
        showLocationDialog,
        onDismiss = { showLocationDialog = false },
        onAttest = { showLocationDialog = false },
    )

    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.Top),
        modifier =
            Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 24.dp)
                .padding(horizontal = 12.dp),
    ) {
        SurfaceSelectionGroupButton(
            items =
                WifiTunnelingItems(
                    uiState,
                    viewModel,
                    currentText,
                    { currentText = it },
                    { isWifiNameReadable() },
                )
        )
        SectionDivider()
        SurfaceSelectionGroupButton(items = NetworkTunnelingItems(uiState, viewModel))
        SectionDivider()
        SurfaceSelectionGroupButton(
            items =
                listOf(
                    AdvancedSettingsItem(
                        onClick = { navController.navigate(Route.AutoTunnelAdvanced) }
                    )
                )
        )
    }
}
