package com.zaneschepke.wireguardautotunnel.ui.screens.main.splittunnel.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.ui.screens.main.splittunnel.state.SplitOption
import com.zaneschepke.wireguardautotunnel.ui.screens.main.splittunnel.state.SplitTunnelUiState

@Composable
fun SplitTunnelContent(
    uiState: SplitTunnelUiState,
    onSplitOptionChange: (SplitOption) -> Unit,
    onAppSelectionToggle: (String) -> Unit,
    onQueryChange: (String) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
    ) {
        SplitOptionSelector(
            selectedOption = uiState.splitOption,
            onOptionChange = onSplitOptionChange,
        )
        if (uiState.splitOption != SplitOption.ALL) {
            AppListSection(
                apps = uiState.queriedApps,
                onAppSelectionToggle = onAppSelectionToggle,
                onQueryChange = onQueryChange,
                uiState.searchQuery,
            )
        }
    }
}
