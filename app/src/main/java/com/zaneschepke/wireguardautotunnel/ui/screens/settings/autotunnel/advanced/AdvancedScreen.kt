package com.zaneschepke.wireguardautotunnel.ui.screens.settings.autotunnel.advanced

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.AppUiState
import com.zaneschepke.wireguardautotunnel.ui.AppViewModel
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItem
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SurfaceSelectionGroupButton
import com.zaneschepke.wireguardautotunnel.ui.common.navigation.TopNavBar
import com.zaneschepke.wireguardautotunnel.util.extensions.scaledHeight
import com.zaneschepke.wireguardautotunnel.util.extensions.scaledWidth

@Composable
fun AdvancedScreen(appUiState: AppUiState, appViewModel: AppViewModel) {

	var isDropDownExpanded by remember {
		mutableStateOf(false)
	}

	var selected by remember { mutableIntStateOf(appUiState.settings.debounceDelaySeconds) }

	LaunchedEffect(selected) {
		if(selected == appUiState.settings.debounceDelaySeconds) return@LaunchedEffect
		appViewModel.saveSettings(appUiState.settings.copy(debounceDelaySeconds = selected))
		if(appUiState.settings.isAutoTunnelEnabled) {
			appViewModel.bounceAutoTunnel()
		}
	}

	Scaffold(
		topBar = {
			TopNavBar(stringResource(R.string.advanced_settings))
		},
	) { padding ->
		Column(
			horizontalAlignment = Alignment.Start,
			verticalArrangement = Arrangement.spacedBy(24.dp.scaledHeight(), Alignment.Top),
			modifier =
			Modifier
				.fillMaxSize()
				.padding(padding)
				.verticalScroll(rememberScrollState())
				.padding(top = 24.dp.scaledHeight())
				.padding(horizontal = 24.dp.scaledWidth()),
		) {
			SurfaceSelectionGroupButton(
				listOf(
					SelectionItem(
						Icons.Outlined.PauseCircle,
						title = {
							Text(
								stringResource(R.string.debounce_delay),
								style = MaterialTheme.typography.bodyMedium.copy(MaterialTheme.colorScheme.onSurface),
							)
						},
						onClick = {
							isDropDownExpanded = true
						},
						trailing = {
							Row(
								horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally),
								verticalAlignment = Alignment.CenterVertically) {
								Text(text = selected.toString(), style = MaterialTheme.typography.bodyMedium)
								val icon = Icons.Default.ArrowDropDown
								Icon(icon, icon.name)
							}
							DropdownMenu(
								modifier = Modifier.height(140.dp.scaledHeight()),
								scrollState = rememberScrollState(),
								containerColor = MaterialTheme.colorScheme.surface,
								expanded = isDropDownExpanded,
								onDismissRequest = {
									isDropDownExpanded = false
								}) {
								(0..10).forEachIndexed { index, num ->
									DropdownMenuItem(text = {
										Text(text = num.toString())
									},
										onClick = {
											isDropDownExpanded = false
											selected = num
										})
								}
							}
						},
					)
				)
			)
		}
	}
}
