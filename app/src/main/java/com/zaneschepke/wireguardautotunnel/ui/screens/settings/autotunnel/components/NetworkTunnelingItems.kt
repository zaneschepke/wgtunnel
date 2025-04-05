package com.zaneschepke.wireguardautotunnel.ui.screens.settings.autotunnel.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AirplanemodeActive
import androidx.compose.material.icons.outlined.SettingsEthernet
import androidx.compose.material.icons.outlined.SignalCellular4Bar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.common.button.ScaledSwitch
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItem
import com.zaneschepke.wireguardautotunnel.ui.state.AppUiState
import com.zaneschepke.wireguardautotunnel.viewmodel.AppViewModel
import com.zaneschepke.wireguardautotunnel.viewmodel.event.AppEvent

@Composable
fun NetworkTunnelingItems(uiState: AppUiState, viewModel: AppViewModel): List<SelectionItem> {
	return listOf(
		SelectionItem(
			leadingIcon = Icons.Outlined.SignalCellular4Bar,
			title = {
				Text(
					stringResource(R.string.tunnel_mobile_data),
					style = MaterialTheme.typography.bodyMedium.copy(MaterialTheme.colorScheme.onSurface),
				)
			},
			trailing = {
				ScaledSwitch(
					enabled = !uiState.appSettings.isAlwaysOnVpnEnabled,
					checked = uiState.appSettings.isTunnelOnMobileDataEnabled,
					onClick = { viewModel.handleEvent(AppEvent.ToggleAutoTunnelOnCellular) },
				)
			},
			onClick = { viewModel.handleEvent(AppEvent.ToggleAutoTunnelOnCellular) },
		),
		SelectionItem(
			leadingIcon = Icons.Outlined.SettingsEthernet,
			title = {
				Text(
					stringResource(R.string.tunnel_on_ethernet),
					style = MaterialTheme.typography.bodyMedium.copy(MaterialTheme.colorScheme.onSurface),
				)
			},
			trailing = {
				ScaledSwitch(
					enabled = !uiState.appSettings.isAlwaysOnVpnEnabled,
					checked = uiState.appSettings.isTunnelOnEthernetEnabled,
					onClick = { viewModel.handleEvent(AppEvent.ToggleAutoTunnelOnEthernet) },
				)
			},
			onClick = { viewModel.handleEvent(AppEvent.ToggleAutoTunnelOnEthernet) },
		),
		SelectionItem(
			leadingIcon = Icons.Outlined.AirplanemodeActive,
			title = {
				Text(
					stringResource(R.string.stop_on_no_internet),
					style = MaterialTheme.typography.bodyMedium.copy(MaterialTheme.colorScheme.onSurface),
				)
			},
			description = {
				Text(
					stringResource(R.string.stop_on_internet_loss),
					style = MaterialTheme.typography.bodySmall.copy(MaterialTheme.colorScheme.outline),
				)
			},
			trailing = {
				ScaledSwitch(
					checked = uiState.appSettings.isStopOnNoInternetEnabled,
					onClick = { viewModel.handleEvent(AppEvent.ToggleStopTunnelOnNoInternet) },
				)
			},
			onClick = { viewModel.handleEvent(AppEvent.ToggleStopTunnelOnNoInternet) },
		),
	)
}
