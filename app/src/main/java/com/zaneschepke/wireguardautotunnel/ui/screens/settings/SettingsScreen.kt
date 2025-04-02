package com.zaneschepke.wireguardautotunnel.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ViewQuilt
import androidx.compose.material.icons.filled.AppShortcut
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.FolderZip
import androidx.compose.material.icons.outlined.Pin
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.VpnKeyOff
import androidx.compose.material.icons.outlined.VpnLock
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.domain.enums.ConfigType
import com.zaneschepke.wireguardautotunnel.ui.Route
import com.zaneschepke.wireguardautotunnel.ui.common.button.ForwardButton
import com.zaneschepke.wireguardautotunnel.ui.common.button.ScaledSwitch
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItem
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SurfaceSelectionGroupButton
import com.zaneschepke.wireguardautotunnel.ui.common.navigation.LocalNavController
import com.zaneschepke.wireguardautotunnel.ui.common.prompt.AuthorizationPrompt
import com.zaneschepke.wireguardautotunnel.ui.common.snackbar.SnackbarController
import com.zaneschepke.wireguardautotunnel.ui.state.AppUiState
import com.zaneschepke.wireguardautotunnel.util.extensions.isRunningOnTv
import com.zaneschepke.wireguardautotunnel.util.extensions.scaledHeight
import com.zaneschepke.wireguardautotunnel.util.extensions.scaledWidth
import com.zaneschepke.wireguardautotunnel.util.extensions.showToast
import com.zaneschepke.wireguardautotunnel.viewmodel.AppViewModel
import com.zaneschepke.wireguardautotunnel.viewmodel.event.AppEvent
import xyz.teamgravity.pin_lock_compose.PinManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(uiState: AppUiState, viewModel: AppViewModel) {
	val context = LocalContext.current
	val navController = LocalNavController.current
	val focusManager = LocalFocusManager.current
	val snackbar = SnackbarController.current
	val isRunningOnTv = remember { context.isRunningOnTv() }

	val interactionSource = remember { MutableInteractionSource() }
	var showAuthPrompt by remember { mutableStateOf(false) }

	var showExportSheet by remember { mutableStateOf(false) }

	if (showAuthPrompt) {
		AuthorizationPrompt(
			onSuccess = {
				showAuthPrompt = false
				showExportSheet = true
			},
			onError = { _ ->
				showAuthPrompt = false
				snackbar.showMessage(
					context.getString(R.string.error_authentication_failed),
				)
			},
			onFailure = {
				showAuthPrompt = false
				snackbar.showMessage(
					context.getString(R.string.error_authorization_failed),
				)
			},
		)
	}

	if (showExportSheet) {
		ModalBottomSheet(onDismissRequest = { showExportSheet = false }) {
			Row(
				modifier =
				Modifier
					.fillMaxWidth()
					.clickable {
						showExportSheet = false
						viewModel.handleEvent(AppEvent.ExportTunnels(ConfigType.AMNEZIA))
					}
					.padding(10.dp),
			) {
				Icon(
					Icons.Filled.FolderZip,
					contentDescription = stringResource(id = R.string.export_amnezia),
					modifier = Modifier.padding(10.dp),
				)
				Text(
					stringResource(id = R.string.export_amnezia),
					modifier = Modifier.padding(10.dp),
				)
			}
			HorizontalDivider()
			Row(
				modifier =
				Modifier
					.fillMaxWidth()
					.clickable {
						showExportSheet = false
						viewModel.handleEvent(AppEvent.ExportTunnels(ConfigType.WG))
					}
					.padding(10.dp),
			) {
				Icon(
					Icons.Filled.FolderZip,
					contentDescription = stringResource(id = R.string.export_wireguard),
					modifier = Modifier.padding(10.dp),
				)
				Text(
					stringResource(id = R.string.export_wireguard),
					modifier = Modifier.padding(10.dp),
				)
			}
		}
	}

	Column(
		horizontalAlignment = Alignment.Start,
		verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.Top),
		modifier =
		Modifier
			.verticalScroll(rememberScrollState())
			.fillMaxSize().systemBarsPadding().imePadding()
			.padding(top = 24.dp.scaledHeight())
			.padding(bottom = 40.dp.scaledHeight())
			.padding(horizontal = 24.dp.scaledWidth())
			.then(
				if (!isRunningOnTv) {
					Modifier.clickable(
						indication = null,
						interactionSource = interactionSource,
					) {
						focusManager.clearFocus()
					}
				} else {
					Modifier
				},
			),
	) {
		val onAutoTunnelClick = {
			if (!uiState.generalState.isLocationDisclosureShown) {
				navController.navigate(Route.LocationDisclosure)
			} else {
				navController.navigate(Route.AutoTunnel)
			}
		}
		SurfaceSelectionGroupButton(
			listOf(
				SelectionItem(
					Icons.Outlined.Bolt,
					title = { Text(stringResource(R.string.auto_tunneling), style = MaterialTheme.typography.bodyMedium.copy(MaterialTheme.colorScheme.onSurface)) },
					description = {
						Text(
							stringResource(R.string.on_demand_rules),
							style = MaterialTheme.typography.bodySmall.copy(MaterialTheme.colorScheme.outline),
						)
					},
					onClick = {
						onAutoTunnelClick()
					},
					trailing = {
						ForwardButton(Modifier.focusable()) { onAutoTunnelClick() }
					},
				),
			),
		)
		SurfaceSelectionGroupButton(
			buildList {
				add(
					SelectionItem(
						Icons.Filled.AppShortcut,
						{
							ScaledSwitch(
								uiState.appSettings.isShortcutsEnabled,
								onClick = { viewModel.handleEvent(AppEvent.ToggleAppShortcuts) },
							)
						},
						title = {
							Text(
								stringResource(R.string.enabled_app_shortcuts),
								style = MaterialTheme.typography.bodyMedium.copy(MaterialTheme.colorScheme.onSurface),
							)
						},
						onClick = { viewModel.handleEvent(AppEvent.ToggleAppShortcuts) },
					),
				)
				if (!isRunningOnTv) {
					add(
						SelectionItem(
							Icons.Outlined.VpnLock,
							{
								ScaledSwitch(
									enabled = !(
										(
											uiState.appSettings.isTunnelOnWifiEnabled ||
												uiState.appSettings.isTunnelOnEthernetEnabled ||
												uiState.appSettings.isTunnelOnMobileDataEnabled
											) &&
											uiState.appSettings.isAutoTunnelEnabled
										),
									onClick = { viewModel.handleEvent(AppEvent.ToggleAlwaysOn) },
									checked = uiState.appSettings.isAlwaysOnVpnEnabled,
								)
							},
							title = {
								Text(
									stringResource(R.string.always_on_vpn_support),
									style = MaterialTheme.typography.bodyMedium.copy(MaterialTheme.colorScheme.onSurface),
								)
							},
							onClick = { viewModel.handleEvent(AppEvent.ToggleAlwaysOn) },
						),
					)
				}
				add(
					SelectionItem(
						Icons.Outlined.VpnKeyOff,
						title = {
							Text(
								stringResource(R.string.kill_switch_options),
								style = MaterialTheme.typography.bodyMedium.copy(MaterialTheme.colorScheme.onSurface),
							)
						},
						onClick = {
							navController.navigate(Route.KillSwitch)
						},
						trailing = {
							ForwardButton { navController.navigate(Route.KillSwitch) }
						},
					),
				)

				add(
					SelectionItem(
						Icons.Outlined.Restore,
						{
							ScaledSwitch(
								uiState.appSettings.isRestoreOnBootEnabled,
								onClick = { viewModel.handleEvent(AppEvent.ToggleRestartAtBoot) },
							)
						},
						title = {
							Text(
								stringResource(R.string.restart_at_boot),
								style = MaterialTheme.typography.bodyMedium.copy(MaterialTheme.colorScheme.onSurface),
							)
						},
						onClick = { viewModel.handleEvent(AppEvent.ToggleRestartAtBoot) },
					),
				)
			},
		)

		fun onPinLockToggle() {
			if (uiState.generalState.isPinLockEnabled) {
				viewModel.handleEvent(AppEvent.TogglePinLock)
			} else {
				PinManager.initialize(context)
				navController.navigate(Route.Lock)
			}
		}

		SurfaceSelectionGroupButton(
			listOf(
				SelectionItem(
					Icons.AutoMirrored.Outlined.ViewQuilt,
					title = { Text(stringResource(R.string.appearance), style = MaterialTheme.typography.bodyMedium.copy(MaterialTheme.colorScheme.onSurface)) },
					onClick = {
						navController.navigate(Route.Appearance)
					},
					trailing = {
						ForwardButton { navController.navigate(Route.Appearance) }
					},
				),
				SelectionItem(
					Icons.Outlined.Pin,
					title = {
						Text(
							stringResource(R.string.enable_app_lock),
							style = MaterialTheme.typography.bodyMedium.copy(MaterialTheme.colorScheme.onSurface),
						)
					},
					trailing = {
						ScaledSwitch(
							uiState.generalState.isPinLockEnabled,
							onClick = {
								onPinLockToggle()
							},
						)
					},
					onClick = {
						onPinLockToggle()
					},
				),
			),
		)

		if (!isRunningOnTv) {
			SurfaceSelectionGroupButton(
				listOf(
					SelectionItem(
						Icons.Outlined.Code,
						title = { Text(stringResource(R.string.kernel), style = MaterialTheme.typography.bodyMedium.copy(MaterialTheme.colorScheme.onSurface)) },
						description = {
							Text(
								stringResource(R.string.use_kernel),
								style = MaterialTheme.typography.bodySmall.copy(MaterialTheme.colorScheme.outline),
							)
						},
						trailing = {
							ScaledSwitch(
								uiState.appSettings.isKernelEnabled,
								onClick = { viewModel.handleEvent(AppEvent.ToggleKernelMode) },
								enabled = !(
									uiState.appSettings.isAutoTunnelEnabled ||
										uiState.appSettings.isAlwaysOnVpnEnabled ||
										uiState.activeTunnels.isNotEmpty()
									),
							)
						},
						onClick = {
							viewModel.handleEvent(AppEvent.ToggleKernelMode)
						},
					),
				),
			)
		}

		if (!isRunningOnTv) {
			SurfaceSelectionGroupButton(
				listOf(
					SelectionItem(
						Icons.Outlined.FolderZip,
						title = {
							Text(
								stringResource(R.string.export_configs),
								style = MaterialTheme.typography.bodyMedium.copy(MaterialTheme.colorScheme.onSurface),
							)
						},
						onClick = {
							if (uiState.tunnels.isEmpty()) return@SelectionItem context.showToast(R.string.tunnel_required)
							showAuthPrompt = true
						},
					),
				),
			)
		}
	}
}
