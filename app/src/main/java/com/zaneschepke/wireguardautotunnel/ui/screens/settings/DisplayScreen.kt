package com.zaneschepke.wireguardautotunnel.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.common.button.IconSurfaceButton
import com.zaneschepke.wireguardautotunnel.ui.common.navigation.TopNavBar
import com.zaneschepke.wireguardautotunnel.ui.state.AppUiState
import com.zaneschepke.wireguardautotunnel.ui.theme.Theme
import com.zaneschepke.wireguardautotunnel.util.extensions.scaledHeight
import com.zaneschepke.wireguardautotunnel.util.extensions.scaledWidth
import com.zaneschepke.wireguardautotunnel.viewmodel.AppViewModel
import com.zaneschepke.wireguardautotunnel.viewmodel.event.AppEvent

@Composable
fun DisplayScreen(appUiState: AppUiState, viewModel: AppViewModel) {
	Scaffold(
		topBar = {
			TopNavBar(stringResource(R.string.display_theme))
		},
	) {
		Column(
			horizontalAlignment = Alignment.Start,
			verticalArrangement = Arrangement.spacedBy(24.dp.scaledHeight(), Alignment.Top),
			modifier =
			Modifier
				.fillMaxSize()
				.padding(it)
				.padding(top = 24.dp.scaledHeight())
				.padding(horizontal = 24.dp.scaledWidth()),
		) {
			IconSurfaceButton(
				title = stringResource(R.string.automatic),
				onClick = {
					viewModel.handleEvent(AppEvent.SetTheme(Theme.AUTOMATIC))
				},
				selected = appUiState.generalState.theme == Theme.AUTOMATIC,
			)
			IconSurfaceButton(
				title = stringResource(R.string.light),
				onClick = { viewModel.handleEvent(AppEvent.SetTheme(Theme.LIGHT)) },
				selected = appUiState.generalState.theme == Theme.LIGHT,
			)
			IconSurfaceButton(
				title = stringResource(R.string.dark),
				onClick = { viewModel.handleEvent(AppEvent.SetTheme(Theme.DARK)) },
				selected = appUiState.generalState.theme == Theme.DARK,
			)
			IconSurfaceButton(
				title = stringResource(R.string.dynamic),
				onClick = { viewModel.handleEvent(AppEvent.SetTheme(Theme.DYNAMIC)) },
				selected = appUiState.generalState.theme == Theme.DYNAMIC,
			)
		}
	}
}
