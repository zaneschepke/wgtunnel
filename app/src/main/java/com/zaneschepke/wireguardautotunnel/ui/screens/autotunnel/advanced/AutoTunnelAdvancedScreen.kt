package com.zaneschepke.wireguardautotunnel.ui.screens.autotunnel.advanced

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.ui.screens.autotunnel.advanced.components.DebounceDelaySelector
import com.zaneschepke.wireguardautotunnel.ui.state.AppUiState
import com.zaneschepke.wireguardautotunnel.viewmodel.AppViewModel

@Composable
fun AutoTunnelAdvancedScreen(appUiState: AppUiState, viewModel: AppViewModel) {
    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.Top),
        modifier =
            Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 24.dp)
                .padding(horizontal = 12.dp),
    ) {
        DebounceDelaySelector(
            currentDelay = appUiState.appSettings.debounceDelaySeconds,
            onEvent = viewModel::handleEvent,
        )
    }
}
