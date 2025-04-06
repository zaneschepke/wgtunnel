package com.zaneschepke.wireguardautotunnel.ui.screens.main.splittunnel

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zaneschepke.wireguardautotunnel.ui.screens.main.splittunnel.components.SplitTunnelContent
import com.zaneschepke.wireguardautotunnel.viewmodel.AppViewModel
import com.zaneschepke.wireguardautotunnel.viewmodel.event.AppEvent

@Composable
fun SplitTunnelScreen(appViewModel: AppViewModel, viewModel: SplitTunnelViewModel = hiltViewModel()) {
	val uiState by viewModel.uiState.collectAsStateWithLifecycle()

	LaunchedEffect(Unit) {
		appViewModel.handleEvent(AppEvent.SetScreenAction { viewModel.saveChanges() })
	}

	Crossfade(
		targetState = uiState.loading,
		animationSpec = tween(200),
		modifier = Modifier
			.fillMaxSize(),
	) { isLoading ->
		if (isLoading) {
			SplitTunnelSkeleton()
		} else {
			SplitTunnelContent(
				uiState = uiState,
				onSplitOptionChange = viewModel::updateSplitOption,
				onAppSelectionToggle = viewModel::toggleAppSelection,
				onQueryChange = viewModel::onSearchQuery,
			)
		}
	}
}
