package com.zaneschepke.wireguardautotunnel.ui.screens.settings

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.domain.entity.AppSettings
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItem
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SurfaceSelectionGroupButton
import com.zaneschepke.wireguardautotunnel.ui.common.navigation.TopNavBar
import com.zaneschepke.wireguardautotunnel.util.extensions.scaledHeight
import com.zaneschepke.wireguardautotunnel.util.extensions.scaledWidth
import com.zaneschepke.wireguardautotunnel.viewmodel.AppViewModel

@Composable
fun AdvancedScreen(appSettings: AppSettings, appViewModel: AppViewModel) {
	var isDropDownExpanded by remember {
		mutableStateOf(false)
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
								verticalAlignment = Alignment.CenterVertically,
							) {
								Text(text = appSettings.debounceDelaySeconds.toString(), style = MaterialTheme.typography.bodyMedium)
								val icon = Icons.Default.ArrowDropDown
								Icon(icon, icon.name)
							}
							DropdownMenu(
								modifier = Modifier.height(250.dp.scaledHeight()),
								scrollState = rememberScrollState(),
								containerColor = MaterialTheme.colorScheme.surface,
								expanded = isDropDownExpanded,
								onDismissRequest = {
									isDropDownExpanded = false
								},
							) {
								(0..10).forEachIndexed { index, num ->
									DropdownMenuItem(
										text = {
											Text(text = num.toString())
										},
										onClick = {
											isDropDownExpanded = false
											appViewModel.saveAppSettings(
												appSettings.copy(debounceDelaySeconds = num),
											)
										},
									)
								}
							}
						},
					),
				),
			)
		}
	}
}
