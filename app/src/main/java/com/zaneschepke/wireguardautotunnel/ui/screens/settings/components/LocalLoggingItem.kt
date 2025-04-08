package com.zaneschepke.wireguardautotunnel.ui.screens.settings.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ViewHeadline
import androidx.compose.runtime.Composable
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.common.button.ScaledSwitch
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItem
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItemLabel
import com.zaneschepke.wireguardautotunnel.ui.state.AppUiState
import com.zaneschepke.wireguardautotunnel.viewmodel.AppViewModel
import com.zaneschepke.wireguardautotunnel.viewmodel.event.AppEvent

@Composable
fun LocalLoggingItem(uiState: AppUiState, viewModel: AppViewModel): SelectionItem {
	return SelectionItem(
		leadingIcon = Icons.Outlined.ViewHeadline,
		title = { SelectionItemLabel(R.string.local_logging) },
		description = { SelectionItemLabel(R.string.enable_local_logging, isDescription = true) },
		trailing = {
			ScaledSwitch(
				checked = uiState.appState.isLocalLogsEnabled,
				onClick = { viewModel.handleEvent(AppEvent.ToggleLocalLogging) },
			)
		},
		onClick = { viewModel.handleEvent(AppEvent.ToggleLocalLogging) },
	)
}
