package com.zaneschepke.wireguardautotunnel.ui.screens.options

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.NetworkPing
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.AppUiState
import com.zaneschepke.wireguardautotunnel.ui.Route
import com.zaneschepke.wireguardautotunnel.ui.common.button.ScaledSwitch
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItem
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SurfaceSelectionGroupButton
import com.zaneschepke.wireguardautotunnel.ui.common.label.GroupLabel
import com.zaneschepke.wireguardautotunnel.ui.common.navigation.LocalNavController
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.autotunnel.components.TrustedNetworkTextBox
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.autotunnel.components.WildcardsLabel
import com.zaneschepke.wireguardautotunnel.ui.theme.iconSize
import com.zaneschepke.wireguardautotunnel.util.extensions.scaledHeight
import com.zaneschepke.wireguardautotunnel.util.extensions.scaledWidth

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OptionsScreen(optionsViewModel: OptionsViewModel = hiltViewModel(), appUiState: AppUiState, tunnelId: Int) {
	val navController = LocalNavController.current
	val config = appUiState.tunnels.first { it.id == tunnelId }

	var currentText by remember { mutableStateOf("") }

	LaunchedEffect(config.tunnelNetworks) {
		currentText = ""
	}
	Scaffold(
		floatingActionButton = {
			ScrollDismissFab(icon = {
				val icon = Icons.Filled.Edit
				Icon(
					imageVector = icon,
					contentDescription = icon.name,
					tint = MaterialTheme.colorScheme.onPrimary,
				)
			}, focusRequester, isVisible = true, onClick = {
				navController.navigate(
					Route.Config(config.id),
				)
			})
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
			GroupLabel(stringResource(R.string.auto_tunneling))
			SurfaceSelectionGroupButton(
				listOf(
					SelectionItem(
						Icons.Outlined.Star,
						title = {
							Text(
								stringResource(R.string.primary_tunnel),
								style = MaterialTheme.typography.bodyMedium.copy(MaterialTheme.colorScheme.onSurface),
							)
						},
						description = {
							Text(
								stringResource(R.string.set_primary_tunnel),
								style = MaterialTheme.typography.bodyMedium.copy(MaterialTheme.colorScheme.outline),
							)
						},
						trailing = {
							ScaledSwitch(
								config.isPrimaryTunnel,
								onClick = { optionsViewModel.onTogglePrimaryTunnel(config) },
							)
						},
						onClick = { optionsViewModel.onTogglePrimaryTunnel(config) },
					),
					SelectionItem(
						Icons.Outlined.PhoneAndroid,
						title = { Text(stringResource(R.string.mobile_tunnel), style = MaterialTheme.typography.bodyMedium.copy(MaterialTheme.colorScheme.onSurface)) },
						description = {
							Text(
								stringResource(R.string.mobile_data_tunnel),
								style = MaterialTheme.typography.bodyMedium.copy(MaterialTheme.colorScheme.outline),
							)
						},
						trailing = {
							ScaledSwitch(
								config.isMobileDataTunnel,
								onClick = { optionsViewModel.onToggleIsMobileDataTunnel(config) },
							)
						},
						onClick = { optionsViewModel.onToggleIsMobileDataTunnel(config) },
					),
					SelectionItem(
						Icons.Outlined.NetworkPing,
						title = {
							Text(
								stringResource(R.string.restart_on_ping),
								style = MaterialTheme.typography.bodyMedium.copy(MaterialTheme.colorScheme.onSurface),
							)
						},
						trailing = {
							ScaledSwitch(
								checked = config.isPingEnabled,
								onClick = { optionsViewModel.onToggleRestartOnPing(config) },
							)
						},
						onClick = { optionsViewModel.onToggleRestartOnPing(config) },
					),
					SelectionItem(
						title = {
							Row(
								verticalAlignment = Alignment.CenterVertically,
								modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp.scaledHeight()),
							) {
								Row(
									verticalAlignment = Alignment.CenterVertically,
									modifier = Modifier
										.weight(4f, false)
										.fillMaxWidth(),
								) {
									val icon = Icons.Outlined.Security
									Icon(
										icon,
										icon.name,
										modifier = Modifier.size(iconSize),
									)
									Column(
										horizontalAlignment = Alignment.Start,
										verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
										modifier = Modifier
											.fillMaxWidth()
											.padding(start = 16.dp.scaledWidth())
											.padding(vertical = 6.dp.scaledHeight()),
									) {
										Text(
											stringResource(R.string.use_tunnel_on_wifi_name),
											style = MaterialTheme.typography.bodyMedium.copy(MaterialTheme.colorScheme.onSurface),
										)
									}
								}
							}
						},
						description = {
							TrustedNetworkTextBox(
								config.tunnelNetworks,
								onDelete = { optionsViewModel.onDeleteRunSSID(it, config) },
								currentText = currentText,
								onSave = { optionsViewModel.onSaveRunSSID(it, config) },
								onValueChange = { currentText = it },
								supporting = {
									if (appUiState.settings.isWildcardsEnabled) {
										WildcardsLabel()
									}
								},
							)
						},
					),
				),
			)
		}
	}
}
