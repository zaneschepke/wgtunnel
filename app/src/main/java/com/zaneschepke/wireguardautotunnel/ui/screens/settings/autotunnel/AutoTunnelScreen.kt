package com.zaneschepke.wireguardautotunnel.ui.screens.settings.autotunnel

import android.Manifest
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.NetworkPing
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.SettingsEthernet
import androidx.compose.material.icons.outlined.SignalCellular4Bar
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.AppUiState
import com.zaneschepke.wireguardautotunnel.ui.common.ClickableIconButton
import com.zaneschepke.wireguardautotunnel.ui.common.button.ScaledSwitch
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItem
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SurfaceSelectionGroupButton
import com.zaneschepke.wireguardautotunnel.ui.common.navigation.TopNavBar
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.autotunnel.components.TrustedNetworkTextBox
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.components.WildcardSupportingLabel
import com.zaneschepke.wireguardautotunnel.ui.theme.iconSize
import com.zaneschepke.wireguardautotunnel.util.extensions.isLocationServicesEnabled
import com.zaneschepke.wireguardautotunnel.util.extensions.isRunningOnTv
import com.zaneschepke.wireguardautotunnel.util.extensions.openWebUrl
import com.zaneschepke.wireguardautotunnel.util.extensions.scaledHeight
import com.zaneschepke.wireguardautotunnel.util.extensions.scaledWidth

@OptIn(ExperimentalPermissionsApi::class, ExperimentalLayoutApi::class)
@Composable
fun AutoTunnelScreen(uiState: AppUiState, viewModel: AutoTunnelViewModel = hiltViewModel()) {
	val context = LocalContext.current

	val fineLocationState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
	var currentText by remember { mutableStateOf("") }
	var isBackgroundLocationGranted by remember { mutableStateOf(true) }
	var showLocationServicesAlertDialog by remember { mutableStateOf(false) }
	var showLocationDialog by remember { mutableStateOf(false) }

	LaunchedEffect(uiState.settings.trustedNetworkSSIDs) {
		currentText = ""
	}

	fun onAutoTunnelWifiChecked() {
		when (false) {
			isBackgroundLocationGranted -> showLocationDialog = true
			fineLocationState.status.isGranted -> showLocationDialog = true
			context.isLocationServicesEnabled() ->
				showLocationServicesAlertDialog = true
			else -> {
				viewModel.onToggleTunnelOnWifi()
			}
		}
	}

	Scaffold(
		topBar = {
			TopNavBar(stringResource(R.string.auto_tunneling))
		},
	) {
		Column(
			horizontalAlignment = Alignment.Start,
			verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.Top),
			modifier =
			Modifier
				.verticalScroll(rememberScrollState())
				.fillMaxSize()
				.padding(top = 24.dp.scaledHeight()).padding(it)
				.padding(horizontal = 24.dp.scaledWidth()),
		) {
			SurfaceSelectionGroupButton(
				mutableListOf(
					SelectionItem(
						Icons.Outlined.Wifi,
						title = { Text(stringResource(R.string.tunnel_on_wifi), style = MaterialTheme.typography.bodyLarge.copy(MaterialTheme.colorScheme.onSurface)) },
						description = {
						},
						trailing = {
							ScaledSwitch(
								enabled = !uiState.settings.isAlwaysOnVpnEnabled,
								checked = uiState.settings.isTunnelOnWifiEnabled,
								onClick = { checked ->
									if (!checked || uiState.isRooted) viewModel.onToggleTunnelOnWifi().also { return@ScaledSwitch }
									onAutoTunnelWifiChecked()
								},
							)
						},
					),
					SelectionItem(
						Icons.Outlined.SignalCellular4Bar,
						title = {
							Text(
								stringResource(R.string.tunnel_mobile_data),
								style = MaterialTheme.typography.bodyLarge.copy(MaterialTheme.colorScheme.onSurface),
							)
						},
						trailing = {
							ScaledSwitch(
								enabled = !uiState.settings.isAlwaysOnVpnEnabled,
								checked = uiState.settings.isTunnelOnMobileDataEnabled,
								onClick = { viewModel.onToggleTunnelOnMobileData() },
							)
						},
					),
					SelectionItem(
						Icons.Outlined.SettingsEthernet,
						title = {
							Text(
								stringResource(R.string.tunnel_on_ethernet),
								style = MaterialTheme.typography.bodyLarge.copy(MaterialTheme.colorScheme.onSurface),
							)
						},
						trailing = {
							ScaledSwitch(
								enabled = !uiState.settings.isAlwaysOnVpnEnabled,
								checked = uiState.settings.isTunnelOnEthernetEnabled,
								onClick = { viewModel.onToggleTunnelOnEthernet() },
							)
						},
					),
					SelectionItem(
						Icons.Outlined.NetworkPing,
						title = {
							Text(
								stringResource(R.string.restart_on_ping),
								style = MaterialTheme.typography.bodyLarge.copy(MaterialTheme.colorScheme.onSurface),
							)
						},
						trailing = {
							ScaledSwitch(
								checked = uiState.settings.isPingEnabled,
 								onClick = { viewModel.onToggleRestartOnPing() },
							)
						},
					),
				).apply {
					if (uiState.settings.isTunnelOnWifiEnabled) {
						add(1,
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
												modifier = Modifier.size(iconSize.scaledWidth()),
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
													"Trusted wifi names",
													style = MaterialTheme.typography.bodyLarge.copy(MaterialTheme.colorScheme.onSurface),
												)
											}

										}
									}

								},
								description = {
									TrustedNetworkTextBox(
										uiState.settings.trustedNetworkSSIDs, onDelete = viewModel::onDeleteTrustedSSID,
										currentText = currentText,
										onSave = viewModel::onSaveTrustedSSID,
										onValueChange = { currentText = it }
									)
								},
							)
						)
					}
				},
			)
		}
	}
}
