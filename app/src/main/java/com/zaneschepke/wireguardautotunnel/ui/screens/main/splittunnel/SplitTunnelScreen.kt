package com.zaneschepke.wireguardautotunnel.ui.screens.main.splittunnel

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.common.navigation.TopNavBar
import com.zaneschepke.wireguardautotunnel.ui.screens.main.splittunnel.components.SplitTunnelContent

@Composable
fun SplitTunnelScreen(viewModel: SplitTunnelViewModel = hiltViewModel()) {
	val uiState by viewModel.uiState.collectAsStateWithLifecycle()

	Scaffold(
		topBar = {
			TopNavBar(
				title = stringResource(R.string.tunneling_apps),
				trailing = {
					IconButton(onClick = { viewModel.saveChanges() }) {
						Icon(
							imageVector = Icons.Outlined.Save,
							contentDescription = stringResource(R.string.save),
						)
					}
				},
			)
		},
	) { padding ->
		Crossfade(
			targetState = uiState.loading,
			animationSpec = tween(200),
			modifier = Modifier
				.fillMaxSize()
				.padding(padding),
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
}
