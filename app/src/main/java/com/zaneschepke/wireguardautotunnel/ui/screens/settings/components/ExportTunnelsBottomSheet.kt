package com.zaneschepke.wireguardautotunnel.ui.screens.settings.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.domain.enums.ConfigType
import com.zaneschepke.wireguardautotunnel.viewmodel.AppViewModel
import com.zaneschepke.wireguardautotunnel.viewmodel.event.AppEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportTunnelsBottomSheet(viewModel: AppViewModel, isRunningOnTv: Boolean) {
	var showAuthPrompt by remember { mutableStateOf(false) }
	var isAuthorized by remember { mutableStateOf(false) }
	var exportType by remember { mutableStateOf(ConfigType.WG) }

	fun handleExport() {
		viewModel.handleEvent(AppEvent.ToggleBottomSheet)
		viewModel.handleEvent(AppEvent.ExportTunnels(exportType))
	}

	if (showAuthPrompt) {
		AuthorizationPromptWrapper(
			onDismiss = { showAuthPrompt = false },
			onSuccess = {
				showAuthPrompt = false
				isAuthorized = true
				handleExport()
			},
			viewModel = viewModel,
		)
	}

	ModalBottomSheet(
		containerColor = MaterialTheme.colorScheme.surface,
		onDismissRequest = { viewModel.handleEvent(AppEvent.ToggleBottomSheet) },
	) {
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.clickable {
					exportType = ConfigType.AMNEZIA
					if (!isAuthorized && !isRunningOnTv) {
						showAuthPrompt = true
						return@clickable
					}
					handleExport()
				}
				.padding(10.dp),
		) {
			Icon(
				Icons.Filled.FolderZip,
				contentDescription = stringResource(R.string.export_tunnels_amnezia),
				modifier = Modifier.padding(10.dp),
			)
			Text(
				stringResource(R.string.export_tunnels_amnezia),
				modifier = Modifier.padding(10.dp),
			)
		}
		HorizontalDivider()
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.clickable {
					exportType = ConfigType.WG
					if (!isAuthorized && !isRunningOnTv) {
						showAuthPrompt = true
						return@clickable
					}
					handleExport()
				}
				.padding(10.dp),
		) {
			Icon(
				Icons.Filled.FolderZip,
				contentDescription = stringResource(R.string.export_tunnels_wireguard),
				modifier = Modifier.padding(10.dp),
			)
			Text(
				stringResource(R.string.export_tunnels_wireguard),
				modifier = Modifier.padding(10.dp),
			)
		}
	}
}
