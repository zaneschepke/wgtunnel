package com.zaneschepke.wireguardautotunnel.ui.screens.autotunnel.disclosure

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.ui.Route
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SurfaceSelectionGroupButton
import com.zaneschepke.wireguardautotunnel.ui.navigation.LocalNavController
import com.zaneschepke.wireguardautotunnel.ui.screens.autotunnel.disclosure.components.AppSettingsItem
import com.zaneschepke.wireguardautotunnel.ui.screens.autotunnel.disclosure.components.LocationDisclosureHeader
import com.zaneschepke.wireguardautotunnel.ui.screens.autotunnel.disclosure.components.SkipItem
import com.zaneschepke.wireguardautotunnel.ui.state.AppUiState
import com.zaneschepke.wireguardautotunnel.util.extensions.goFromRoot
import com.zaneschepke.wireguardautotunnel.viewmodel.AppViewModel

@Composable
fun LocationDisclosureScreen(appUiState: AppUiState, viewModel: AppViewModel) {
    val navController = LocalNavController.current

    LaunchedEffect(Unit, appUiState) {
        if (appUiState.appState.isLocationDisclosureShown)
            navController.goFromRoot(Route.AutoTunnel)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp, Alignment.Top),
        modifier = Modifier.fillMaxSize().padding(top = 18.dp).padding(horizontal = 24.dp),
    ) {
        LocationDisclosureHeader()
        SurfaceSelectionGroupButton(items = listOf(AppSettingsItem(viewModel)))
        SurfaceSelectionGroupButton(items = listOf(SkipItem(viewModel)))
    }
}
