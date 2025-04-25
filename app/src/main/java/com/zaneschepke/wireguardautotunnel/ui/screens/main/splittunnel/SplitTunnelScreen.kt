package com.zaneschepke.wireguardautotunnel.ui.screens.main.splittunnel

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.screens.main.splittunnel.components.SplitTunnelContent
import com.zaneschepke.wireguardautotunnel.util.StringValue
import com.zaneschepke.wireguardautotunnel.viewmodel.AppViewModel
import com.zaneschepke.wireguardautotunnel.viewmodel.event.AppEvent

@Composable
fun SplitTunnelScreen(
    appViewModel: AppViewModel,
    viewModel: SplitTunnelViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        appViewModel.handleEvent(AppEvent.SetScreenAction { viewModel.saveChanges() })
    }

    LaunchedEffect(uiState.success) {
        if (uiState.success == true) {
            appViewModel.handleEvent(
                AppEvent.ShowMessage(StringValue.StringResource(R.string.config_changes_saved))
            )
            appViewModel.handleEvent(AppEvent.PopBackStack(true))
        }
    }
    if (uiState.loading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(modifier = Modifier.size(30.dp), strokeWidth = 5.dp)
        }
    } else {
        SplitTunnelContent(
            uiState = uiState,
            onSplitOptionChange = viewModel::updateSplitOption,
            onAppSelectionToggle = viewModel::toggleAppSelection,
            onQueryChange = viewModel::onSearchQuery,
        )
    }
}
