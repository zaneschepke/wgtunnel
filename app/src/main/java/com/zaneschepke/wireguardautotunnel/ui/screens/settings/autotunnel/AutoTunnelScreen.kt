package com.zaneschepke.wireguardautotunnel.ui.screens.settings.autotunnel

import android.Manifest
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AirplanemodeActive
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Filter1
import androidx.compose.material.icons.outlined.NetworkPing
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SettingsEthernet
import androidx.compose.material.icons.outlined.SignalCellular4Bar
import androidx.compose.material.icons.outlined.VpnKeyOff
import androidx.compose.material.icons.outlined.Wifi
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.AppUiState
import com.zaneschepke.wireguardautotunnel.ui.Route
import com.zaneschepke.wireguardautotunnel.ui.common.button.ScaledSwitch
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItem
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SurfaceSelectionGroupButton
import com.zaneschepke.wireguardautotunnel.ui.common.navigation.LocalNavController
import com.zaneschepke.wireguardautotunnel.ui.common.navigation.TopNavBar
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.autotunnel.components.TrustedNetworkTextBox
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.autotunnel.components.WildcardsLabel
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.components.BackgroundLocationDialog
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.components.ForwardButton
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.components.LearnMoreLinkLabel
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.components.LocationServicesDialog
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
	val navController = LocalNavController.current

	val fineLocationState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
	var currentText by remember { mutableStateOf("") }
	var isBackgroundLocationGranted by remember { mutableStateOf(true) }
	var showLocationServicesAlertDialog by remember { mutableStateOf(false) }
	var showLocationDialog by remember { mutableStateOf(false) }

	fun checkFineLocationGranted() {
		isBackgroundLocationGranted = fineLocationState.status.isGranted
	}

	fun isWifiNameReadable(): Boolean {
		return when {
			!isBackgroundLocationGranted ||
				!fineLocationState.status.isGranted -> {
				showLocationDialog = true
				false
			}
			!context.isLocationServicesEnabled() -> {
				showLocationServicesAlertDialog = true
				false
			}
			else -> true
		}
	}

	if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) checkFineLocationGranted()
	if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
		if (context.isRunningOnTv() && Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
			checkFineLocationGranted()
		} else {
			val backgroundLocationState = rememberPermissionState(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
			isBackgroundLocationGranted = backgroundLocationState.status.isGranted
		}
	}

	LaunchedEffect(uiState.settings.trustedNetworkSSIDs) {
		currentText = ""
	}

	LocationServicesDialog(
		showLocationServicesAlertDialog,
		onDismiss = { showLocationServicesAlertDialog = false },
		onAttest = {
			showLocationServicesAlertDialog = false
		},
	)

	BackgroundLocationDialog(
		showLocationDialog,
		onDismiss = { showLocationDialog = false },
		onAttest = { showLocationDialog = false },
	)

	Scaffold(
		topBar = {
			TopNavBar(stringResource(R.string.auto_tunneling))
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
				buildList {
					addAll(
						listOf(
							SelectionItem(
								Icons.Outlined.Wifi,
								title = {
									Text(
										stringResource(R.string.tunnel_on_wifi),
										style = MaterialTheme.typography.bodyMedium.copy(MaterialTheme.colorScheme.onSurface),
									)
								},
								description = {
								},
								trailing = {
									ScaledSwitch(
										enabled = !uiState.settings.isAlwaysOnVpnEnabled,
										checked = uiState.settings.isTunnelOnWifiEnabled,
										onClick = {
											viewModel.onToggleTunnelOnWifi()
										},
									)
								},
								onClick = {
									viewModel.onToggleTunnelOnWifi()
								},
							),
							SelectionItem(
								Icons.Outlined.Code,
								title = {
									Text(
										stringResource(R.string.wifi_name_via_shell),
										style = MaterialTheme.typography.bodyMedium.copy(MaterialTheme.colorScheme.onSurface),
									)
								},
								description = {
									Text(
										stringResource(R.string.use_root_shell_for_wifi),
										style = MaterialTheme.typography.bodySmall.copy(MaterialTheme.colorScheme.outline),
									)
								},
								trailing = {
									ScaledSwitch(
										checked = uiState.settings.isWifiNameByShellEnabled,
										onClick = {
											viewModel.onRootShellWifiToggle()
										},
									)
								},
								onClick = {
									viewModel.onRootShellWifiToggle()
								},
							),
						),
					)
					if (uiState.settings.isTunnelOnWifiEnabled) {
						addAll(
							listOf(
								SelectionItem(
									Icons.Outlined.Filter1,
									title = {
										Text(
											stringResource(R.string.use_wildcards),
											style = MaterialTheme.typography.bodyMedium.copy(MaterialTheme.colorScheme.onSurface),
										)
									},
									description = {
										LearnMoreLinkLabel({ context.openWebUrl(it) }, stringResource(id = R.string.docs_wildcards))
									},
									trailing = {
										ScaledSwitch(
											checked = uiState.settings.isWildcardsEnabled,
											onClick = {
												viewModel.onToggleWildcards()
											},
										)
									},
									onClick = {
										viewModel.onToggleWildcards()
									},
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
														stringResource(R.string.trusted_wifi_names),
														style = MaterialTheme.typography.bodyMedium.copy(MaterialTheme.colorScheme.onSurface),
													)
												}
											}
										}
									},
									description = {
										TrustedNetworkTextBox(
											uiState.settings.trustedNetworkSSIDs,
											onDelete = viewModel::onDeleteTrustedSSID,
											currentText = currentText,
											onSave = { ssid ->
												if (uiState.settings.isWifiNameByShellEnabled || isWifiNameReadable()) viewModel.onSaveTrustedSSID(ssid)
											},
											onValueChange = { currentText = it },
											supporting = {
												if (uiState.settings.isWildcardsEnabled) {
													WildcardsLabel()
												}
											},
										)
									},
								),
							),
						)
					}
				},
			)
			SurfaceSelectionGroupButton(
				listOf(
					SelectionItem(
						Icons.Outlined.SignalCellular4Bar,
						title = {
							Text(
								stringResource(R.string.tunnel_mobile_data),
								style = MaterialTheme.typography.bodyMedium.copy(MaterialTheme.colorScheme.onSurface),
							)
						},
						trailing = {
							ScaledSwitch(
								enabled = !uiState.settings.isAlwaysOnVpnEnabled,
								checked = uiState.settings.isTunnelOnMobileDataEnabled,
								onClick = { viewModel.onToggleTunnelOnMobileData() },
							)
						},
						onClick = {
							viewModel.onToggleTunnelOnMobileData()
						},
					),
					SelectionItem(
						Icons.Outlined.SettingsEthernet,
						title = {
							Text(
								stringResource(R.string.tunnel_on_ethernet),
								style = MaterialTheme.typography.bodyMedium.copy(MaterialTheme.colorScheme.onSurface),
							)
						},
						trailing = {
							ScaledSwitch(
								enabled = !uiState.settings.isAlwaysOnVpnEnabled,
								checked = uiState.settings.isTunnelOnEthernetEnabled,
								onClick = { viewModel.onToggleTunnelOnEthernet() },
							)
						},
						onClick = {
							viewModel.onToggleTunnelOnEthernet()
						},
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
								checked = uiState.settings.isPingEnabled,
								onClick = { viewModel.onToggleRestartOnPing() },
							)
						},
						onClick = {
							viewModel.onToggleRestartOnPing()
						},
					),
					SelectionItem(
						Icons.Outlined.AirplanemodeActive,
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
								checked = uiState.settings.isStopOnNoInternetEnabled,
								onClick = { viewModel.onToggleStopOnNoInternet() },
							)
						},
						onClick = {
							viewModel.onToggleStopOnNoInternet()
						},
					),
				),
			)
			SurfaceSelectionGroupButton(
				listOf(
					SelectionItem(
						Icons.Outlined.Settings,
						title = {
							Text(
								stringResource(R.string.advanced_settings),
								style = MaterialTheme.typography.bodyMedium.copy(MaterialTheme.colorScheme.onSurface),
							)
						},
						onClick = {
							navController.navigate(Route.AutoTunnelAdvanced)
						},
						trailing = {
							ForwardButton { navController.navigate(Route.AutoTunnelAdvanced) }
						},
					),
				)
			)
		}
	}
}
