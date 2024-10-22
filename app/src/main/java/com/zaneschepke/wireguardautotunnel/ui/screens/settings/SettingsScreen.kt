package com.zaneschepke.wireguardautotunnel.ui.screens.settings

import android.content.Context.POWER_SERVICE
import android.content.Intent
import android.net.Uri
import android.net.VpnService
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity.RESULT_OK
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.ArrowRight
import androidx.compose.material.icons.automirrored.outlined.ViewQuilt
import androidx.compose.material.icons.filled.AppShortcut
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.VpnLock
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.AppShortcut
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.FolderZip
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Pin
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.VpnLock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.WireGuardAutoTunnel
import com.zaneschepke.wireguardautotunnel.service.tunnel.TunnelState
import com.zaneschepke.wireguardautotunnel.ui.AppUiState
import com.zaneschepke.wireguardautotunnel.ui.AppViewModel
import com.zaneschepke.wireguardautotunnel.ui.Route
import com.zaneschepke.wireguardautotunnel.ui.common.button.ScaledSwitch
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItem
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SurfaceSelectionGroupButton
import com.zaneschepke.wireguardautotunnel.ui.common.navigation.LocalNavController
import com.zaneschepke.wireguardautotunnel.ui.common.prompt.AuthorizationPrompt
import com.zaneschepke.wireguardautotunnel.ui.common.snackbar.SnackbarController
import com.zaneschepke.wireguardautotunnel.ui.screens.main.components.VpnDeniedDialog
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.components.BackgroundLocationDialog
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.components.BackgroundLocationDisclosure
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.components.LocationServicesDialog
import com.zaneschepke.wireguardautotunnel.util.extensions.isRunningOnTv
import com.zaneschepke.wireguardautotunnel.util.extensions.launchAppSettings
import com.zaneschepke.wireguardautotunnel.util.extensions.launchNotificationSettings
import com.zaneschepke.wireguardautotunnel.util.extensions.launchVpnSettings
import com.zaneschepke.wireguardautotunnel.util.extensions.scaledHeight
import com.zaneschepke.wireguardautotunnel.util.extensions.scaledWidth
import com.zaneschepke.wireguardautotunnel.util.extensions.showToast
import xyz.teamgravity.pin_lock_compose.PinManager

@OptIn(
	ExperimentalPermissionsApi::class,
	ExperimentalLayoutApi::class,
)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel(), appViewModel: AppViewModel, uiState: AppUiState, focusRequester: FocusRequester) {
	val context = LocalContext.current
	val navController = LocalNavController.current
	val focusManager = LocalFocusManager.current
	val snackbar = SnackbarController.current

	val scrollState = rememberScrollState()
	val interactionSource = remember { MutableInteractionSource() }

	val settingsUiState by viewModel.uiState.collectAsStateWithLifecycle()

	var showVpnPermissionDialog by remember { mutableStateOf(false) }
	var showLocationServicesAlertDialog by remember { mutableStateOf(false) }
	var showAuthPrompt by remember { mutableStateOf(false) }
	var showLocationDialog by remember { mutableStateOf(false) }

	val startForResult =
		rememberLauncherForActivityResult(
			ActivityResultContracts.StartActivityForResult(),
		) { result: ActivityResult ->
			if (result.resultCode == RESULT_OK) {
				result.data
				// Handle the Intent
			}
			viewModel.setBatteryOptimizeDisableShown()
		}

	val vpnActivityResultState =
		rememberLauncherForActivityResult(
			ActivityResultContracts.StartActivityForResult(),
			onResult = {
				val accepted = (it.resultCode == RESULT_OK)
				if (accepted) {
					viewModel.onToggleAutoTunnel(context)
				} else {
					showVpnPermissionDialog = true
				}
			},
		)

	fun isBatteryOptimizationsDisabled(): Boolean {
		val pm = context.getSystemService(POWER_SERVICE) as PowerManager
		return pm.isIgnoringBatteryOptimizations(context.packageName)
	}

	fun requestBatteryOptimizationsDisabled() {
		val intent =
			Intent().apply {
				action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
				data = Uri.parse("package:${context.packageName}")
			}
		startForResult.launch(intent)
	}

	fun handleAutoTunnelToggle() {
		if (!uiState.generalState.isBatteryOptimizationDisableShown &&
			!isBatteryOptimizationsDisabled() && !context.isRunningOnTv()
		) {
			return requestBatteryOptimizationsDisabled()
		}
		val intent = if (!uiState.settings.isKernelEnabled) {
			VpnService.prepare(context)
		} else {
			null
		}
		if (intent != null) return vpnActivityResultState.launch(intent)
		viewModel.onToggleAutoTunnel(context)
	}

// 	fun checkFineLocationGranted() {
// 		isBackgroundLocationGranted =
// 			if (!fineLocationState.status.isGranted) {
// 				false
// 			} else {
// 				viewModel.setLocationDisclosureShown()
// 				true
// 			}
// 	}

// 	if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
// 		if (
// 			isRunningOnTv &&
// 			Build.VERSION.SDK_INT == Build.VERSION_CODES.Q
// 		) {
// 			checkFineLocationGranted()
// 		} else {
// 			val backgroundLocationState =
// 				rememberPermissionState(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
// 			isBackgroundLocationGranted =
// 				if (!backgroundLocationState.status.isGranted) {
// 					false
// 				} else {
// 					SideEffect { viewModel.setLocationDisclosureShown() }
// 					true
// 				}
// 		}
// 	}
//
// 	if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
// 		checkFineLocationGranted()
// 	}
	if (!uiState.generalState.isLocationDisclosureShown) {
		BackgroundLocationDisclosure(
			onDismiss = { viewModel.setLocationDisclosureShown() },
			onAttest = {
				context.launchAppSettings()
				viewModel.setLocationDisclosureShown()
			},
			scrollState,
			focusRequester,
		)
		return
	}

	BackgroundLocationDialog(
		showLocationDialog,
		onDismiss = { showLocationDialog = false },
		onAttest = { showLocationDialog = false },
	)

	LocationServicesDialog(
		showLocationServicesAlertDialog,
		onDismiss = { showVpnPermissionDialog = false },
		onAttest = { handleAutoTunnelToggle() },
	)

	VpnDeniedDialog(showVpnPermissionDialog, onDismiss = { showVpnPermissionDialog = false })

	if (showAuthPrompt) {
		AuthorizationPrompt(
			onSuccess = {
				showAuthPrompt = false
				viewModel.exportAllConfigs(context)
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

	Column(
		horizontalAlignment = Alignment.Start,
		verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.Top),
		modifier =
		Modifier
			.verticalScroll(rememberScrollState())
			.fillMaxSize()
			.padding(top = 24.dp.scaledHeight())
			.padding(horizontal = 24.dp.scaledWidth()).clickable(
				indication = null,
				interactionSource = interactionSource,
			) {
				focusManager.clearFocus()
			}.windowInsetsPadding(WindowInsets.systemBars),
	) {
		SurfaceSelectionGroupButton(
			listOf(
				SelectionItem(
					Icons.Outlined.Bolt,
					title = { Text(stringResource(R.string.auto_tunneling), style = MaterialTheme.typography.bodyLarge.copy(MaterialTheme.colorScheme.onSurface)) },
					description = {
						Text(
							"Configure on demand tunnel rules",
							style = MaterialTheme.typography.bodyMedium.copy(MaterialTheme.colorScheme.outline),
						)
					},
					onClick = {
						navController.navigate(Route.AutoTunnel)
					},
					trailing = {
						val icon = Icons.AutoMirrored.Outlined.ArrowForward
						Icon(icon, icon.name)
					}
				),
			),
		)

		SurfaceSelectionGroupButton(
			listOf(
				SelectionItem(
					Icons.Filled.AppShortcut,
					{
						ScaledSwitch(
							uiState.settings.isShortcutsEnabled,
							onClick = { viewModel.onToggleShortcutsEnabled() },
						)
					},
					title = {
						Text(stringResource(R.string.enabled_app_shortcuts), style = MaterialTheme.typography.bodyLarge.copy(MaterialTheme.colorScheme.onSurface))
					},
				),
				SelectionItem(
					Icons.Outlined.VpnLock,
					{
						ScaledSwitch(
							enabled = !(
								(
									uiState.settings.isTunnelOnWifiEnabled ||
										uiState.settings.isTunnelOnEthernetEnabled ||
										uiState.settings.isTunnelOnMobileDataEnabled
									) &&
									uiState.settings.isAutoTunnelEnabled
								),
							onClick = { viewModel.onToggleAlwaysOnVPN() },
							checked = uiState.settings.isAlwaysOnVpnEnabled,
						)
					},
					title = {
						Text(stringResource(R.string.always_on_vpn_support), style = MaterialTheme.typography.bodyLarge.copy(MaterialTheme.colorScheme.onSurface))
					},
				),
				SelectionItem(
					Icons.Outlined.AdminPanelSettings,
					title = { Text(stringResource(R.string.kill_switch), style = MaterialTheme.typography.bodyLarge.copy(MaterialTheme.colorScheme.onSurface)) },
					onClick = {
						context.launchVpnSettings()
					},
					trailing = {
						val icon = Icons.AutoMirrored.Outlined.ArrowForward
						Icon(icon, icon.name)
					}
				),
				SelectionItem(
					Icons.Outlined.Restore,
					{
						ScaledSwitch(
							uiState.settings.isRestoreOnBootEnabled,
							onClick = { viewModel.onToggleRestartAtBoot() },
						)
					},
					title = { Text(stringResource(R.string.restart_at_boot), style = MaterialTheme.typography.bodyLarge.copy(MaterialTheme.colorScheme.onSurface)) },
				),
			),
		)

		SurfaceSelectionGroupButton(
			mutableListOf(
				SelectionItem(
					Icons.AutoMirrored.Outlined.ViewQuilt,
					title = { Text(stringResource(R.string.appearance), style = MaterialTheme.typography.bodyLarge.copy(MaterialTheme.colorScheme.onSurface)) },
					onClick = {
						navController.navigate(Route.Appearance)
					},
					trailing = {
						val icon = Icons.AutoMirrored.Outlined.ArrowForward
						Icon(icon, icon.name)
					}
				),
				SelectionItem(
					Icons.Outlined.Notifications,
					title = { Text(stringResource(R.string.notifications), style = MaterialTheme.typography.bodyLarge.copy(MaterialTheme.colorScheme.onSurface)) },
					onClick = {
						context.launchNotificationSettings()
					},
					trailing = {
						val icon = Icons.AutoMirrored.Outlined.ArrowForward
						Icon(icon, icon.name)
					}
				),
				SelectionItem(
					Icons.Outlined.Pin,
					title = { Text(stringResource(R.string.enable_app_lock), style = MaterialTheme.typography.bodyLarge.copy(MaterialTheme.colorScheme.onSurface)) },
					trailing = {
						ScaledSwitch(
							uiState.generalState.isPinLockEnabled,
							onClick = {
								if (uiState.generalState.isPinLockEnabled) {
									appViewModel.onPinLockDisabled()
								} else {
									PinManager.initialize(context)
									navController.navigate(Route.Lock)
								}
							},
						)
					}
				),
			),
		)

		SurfaceSelectionGroupButton(
			listOf(
				SelectionItem(
					Icons.Outlined.Code,
					title = { Text(stringResource(R.string.kernel), style = MaterialTheme.typography.bodyLarge.copy(MaterialTheme.colorScheme.onSurface)) },
					description = {
						Text(
							"Use kernel backend (root only)",
							style = MaterialTheme.typography.bodyMedium.copy(MaterialTheme.colorScheme.outline),
						)
					},
					trailing = {
						ScaledSwitch(
							uiState.settings.isKernelEnabled,
							onClick = { viewModel.onToggleKernelMode() },
							enabled = !(
								uiState.settings.isAutoTunnelEnabled ||
									uiState.settings.isAlwaysOnVpnEnabled ||
									(uiState.vpnState.status == TunnelState.UP) ||
									!settingsUiState.isKernelAvailable
								),
						)
					},
				),
			),
		)

		SurfaceSelectionGroupButton(
			listOf(
				SelectionItem(
					Icons.Outlined.FolderZip,
					title = { Text(stringResource(R.string.export_configs), style = MaterialTheme.typography.bodyLarge.copy(MaterialTheme.colorScheme.onSurface)) },
					onClick = {
						if (uiState.tunnels.isEmpty()) return@SelectionItem context.showToast(R.string.tunnel_required)
						showAuthPrompt = true
					},
					trailing = {},
				),
			),
		)

// 		Surface(
// 			tonalElevation = 2.dp,
// 			shadowElevation = 2.dp,
// 			shape = RoundedCornerShape(12.dp),
// 			color = MaterialTheme.colorScheme.surface,
// 			modifier =
// 			(
// 				if (isRunningOnTv) {
// 					Modifier
// 						.height(IntrinsicSize.Min)
// 						.fillMaxWidth(fillMaxWidth)
// 						.padding(top = 10.dp)
// 				} else {
// 					Modifier
// 						.fillMaxWidth(fillMaxWidth)
// 						.padding(top = 20.dp)
// 				}
// 				)
// 				.padding(bottom = 10.dp),
// 		) {
// 			Column(
// 				horizontalAlignment = Alignment.Start,
// 				verticalArrangement = Arrangement.Top,
// 				modifier = Modifier.padding(15.dp),
// 			) {
// 				SectionTitle(
// 					title = stringResource(id = R.string.auto_tunneling),
// 					padding = screenPadding,
// 				)
// 				ConfigurationToggle(
// 					stringResource(id = R.string.tunnel_on_wifi),
// 					enabled = !uiState.settings.isAlwaysOnVpnEnabled,
// 					checked = uiState.settings.isTunnelOnWifiEnabled,
// 					onCheckChanged = { checked ->
// 						if (!checked || settingsUiState.isRooted) viewModel.onToggleTunnelOnWifi().also { return@ConfigurationToggle }
// 						onAutoTunnelWifiChecked()
// 					},
// 					modifier =
// 					if (uiState.settings.isAutoTunnelEnabled) {
// 						Modifier
// 					} else {
// 						Modifier
// 							.focusRequester(focusRequester)
// 					},
// 				)
// 				if (uiState.settings.isTunnelOnWifiEnabled) {
// 					Column {
// 						FlowRow(
// 							modifier =
// 							Modifier
// 								.padding(screenPadding)
// 								.fillMaxWidth(),
// 							horizontalArrangement = Arrangement.spacedBy(5.dp),
// 						) {
// 							uiState.settings.trustedNetworkSSIDs.forEach { ssid ->
// 								ClickableIconButton(
// 									onClick = {
// 										if (isRunningOnTv) {
// 											focusRequester.requestFocus()
// 											viewModel.onDeleteTrustedSSID(ssid)
// 										}
// 									},
// 									onIconClick = {
// 										if (isRunningOnTv) focusRequester.requestFocus()
// 										viewModel.onDeleteTrustedSSID(ssid)
// 									},
// 									text = ssid,
// 									icon = Icons.Filled.Close,
// 								)
// 							}
// 							if (uiState.settings.trustedNetworkSSIDs.isEmpty()) {
// 								Text(
// 									stringResource(R.string.none),
// 									fontStyle = FontStyle.Italic,
// 									style = MaterialTheme.typography.bodySmall,
// 									color = MaterialTheme.colorScheme.onSurface,
// 								)
// 							}
// 						}
// 						OutlinedTextField(
// 							value = currentText,
// 							onValueChange = { currentText = it },
// 							label = { Text(stringResource(R.string.add_trusted_ssid)) },
// 							modifier =
// 							Modifier
// 								.padding(
// 									start = screenPadding,
// 									top = 5.dp,
// 									bottom = 10.dp,
// 								),
// 							supportingText = { WildcardSupportingLabel { context.openWebUrl(it) } },
// 							maxLines = 1,
// 							keyboardOptions =
// 							KeyboardOptions(
// 								capitalization = KeyboardCapitalization.None,
// 								imeAction = ImeAction.Done,
// 							),
// 							keyboardActions = KeyboardActions(onDone = { saveTrustedSSID() }),
// 							trailingIcon = {
// 								if (currentText != "") {
// 									IconButton(onClick = { saveTrustedSSID() }) {
// 										Icon(
// 											imageVector = Icons.Outlined.Add,
// 											contentDescription =
// 											if (currentText == "") {
// 												stringResource(
// 													id =
// 													R.string
// 														.trusted_ssid_empty_description,
// 												)
// 											} else {
// 												stringResource(
// 													id =
// 													R.string
// 														.trusted_ssid_value_description,
// 												)
// 											},
// 											tint = MaterialTheme.colorScheme.primary,
// 										)
// 									}
// 								}
// 							},
// 						)
// 					}
// 				}
// 				ConfigurationToggle(
// 					stringResource(R.string.tunnel_mobile_data),
// 					enabled = !uiState.settings.isAlwaysOnVpnEnabled,
// 					checked = uiState.settings.isTunnelOnMobileDataEnabled,
// 					onCheckChanged = { viewModel.onToggleTunnelOnMobileData() },
// 				)
// 				ConfigurationToggle(
// 					stringResource(id = R.string.tunnel_on_ethernet),
// 					enabled = !uiState.settings.isAlwaysOnVpnEnabled,
// 					checked = uiState.settings.isTunnelOnEthernetEnabled,
// 					onCheckChanged = { viewModel.onToggleTunnelOnEthernet() },
// 				)
// 				ConfigurationToggle(
// 					stringResource(R.string.restart_on_ping),
// 					checked = uiState.settings.isPingEnabled,
// 					onCheckChanged = { viewModel.onToggleRestartOnPing() },
// 				)
// 				Row(
// 					verticalAlignment = Alignment.CenterVertically,
// 					modifier =
// 					(
// 						if (!uiState.settings.isAutoTunnelEnabled) {
// 							Modifier
// 						} else {
// 							Modifier.focusRequester(
// 								focusRequester,
// 							)
// 						}
// 						)
// 						.fillMaxSize()
// 						.padding(top = 5.dp),
// 					horizontalArrangement = Arrangement.Center,
// 				) {
// 					TextButton(
// 						onClick = {
// 							if (uiState.tunnels.isEmpty()) return@TextButton context.showToast(R.string.tunnel_required)
// 							handleAutoTunnelToggle()
// 						},
// 					) {
// 						val autoTunnelButtonText =
// 							if (uiState.settings.isAutoTunnelEnabled) {
// 								stringResource(R.string.disable_auto_tunnel)
// 							} else {
// 								stringResource(id = R.string.enable_auto_tunnel)
// 							}
// 						Text(autoTunnelButtonText)
// 					}
// 				}
// 			}
// 		}
// 		Surface(
// 			tonalElevation = 2.dp,
// 			shadowElevation = 2.dp,
// 			shape = RoundedCornerShape(12.dp),
// 			color = MaterialTheme.colorScheme.surface,
// 			modifier =
// 			Modifier
// 				.fillMaxWidth(fillMaxWidth)
// 				.padding(vertical = 10.dp),
// 		) {
// 			Column(
// 				horizontalAlignment = Alignment.Start,
// 				verticalArrangement = Arrangement.Top,
// 				modifier = Modifier.padding(15.dp),
// 			) {
// 				SectionTitle(
// 					title = stringResource(id = R.string.backend),
// 					padding = screenPadding,
// 				)
// 				ConfigurationToggle(
// 					stringResource(R.string.use_kernel),
// 					enabled =
// 					!(
// 						uiState.settings.isAutoTunnelEnabled ||
// 							uiState.settings.isAlwaysOnVpnEnabled ||
// 							(uiState.vpnState.status == TunnelState.UP) ||
// 							!settingsUiState.isKernelAvailable
// 						),
// 					checked = uiState.settings.isKernelEnabled,
// 					onCheckChanged = {
// 						viewModel.onToggleKernelMode()
// 					},
// 				)
// 				Row(
// 					verticalAlignment = Alignment.CenterVertically,
// 					modifier =
// 					Modifier
// 						.fillMaxSize()
// 						.padding(top = 5.dp),
// 					horizontalArrangement = Arrangement.Center,
// 				) {
// 					TextButton(
// 						onClick = {
// 							viewModel.onRequestRoot()
// 						},
// 					) {
// 						Text(stringResource(R.string.request_root))
// 					}
// 				}
// 			}
// 		}
// 		Surface(
// 			tonalElevation = 2.dp,
// 			shadowElevation = 2.dp,
// 			shape = RoundedCornerShape(12.dp),
// 			color = MaterialTheme.colorScheme.surface,
// 			modifier =
// 			Modifier
// 				.fillMaxWidth(fillMaxWidth)
// 				.padding(vertical = 10.dp)
// 				.padding(bottom = 10.dp),
// 		) {
// 			Column(
// 				horizontalAlignment = Alignment.Start,
// 				verticalArrangement = Arrangement.Top,
// 				modifier = Modifier.padding(15.dp),
// 			) {
// 				SectionTitle(
// 					title = stringResource(id = R.string.other),
// 					padding = screenPadding,
// 				)
// 				if (!isRunningOnTv) {
// 					ConfigurationToggle(
// 						stringResource(R.string.always_on_vpn_support),
// 						enabled = !(
// 							(
// 								uiState.settings.isTunnelOnWifiEnabled ||
// 									uiState.settings.isTunnelOnEthernetEnabled ||
// 									uiState.settings.isTunnelOnMobileDataEnabled
// 								) &&
// 								uiState.settings.isAutoTunnelEnabled
// 							),
// 						checked = uiState.settings.isAlwaysOnVpnEnabled,
// 						onCheckChanged = { viewModel.onToggleAlwaysOnVPN() },
// 					)
// 					ConfigurationToggle(
// 						stringResource(R.string.enabled_app_shortcuts),
// 						enabled = true,
// 						checked = uiState.settings.isShortcutsEnabled,
// 						onCheckChanged = { viewModel.onToggleShortcutsEnabled() },
// 					)
// 				}
// 				ConfigurationToggle(
// 					stringResource(R.string.restart_at_boot),
// 					enabled = true,
// 					checked = uiState.settings.isRestoreOnBootEnabled,
// 					onCheckChanged = {
// 						viewModel.onToggleRestartAtBoot()
// 					},
// 				)
// 				ConfigurationToggle(
// 					stringResource(R.string.enable_app_lock),
// 					enabled = true,
// 					checked = uiState.generalState.isPinLockEnabled,
// 					onCheckChanged = {
// 						if (uiState.generalState.isPinLockEnabled) {
// 							appViewModel.onPinLockDisabled()
// 						} else {
// 							// TODO may want to show a dialog before proceeding in the future
// 							PinManager.initialize(WireGuardAutoTunnel.instance)
// 							navController.navigate(Route.Lock)
// 						}
// 					},
// 				)
// 				if (!isRunningOnTv) {
// 					Row(
// 						verticalAlignment = Alignment.CenterVertically,
// 						modifier =
// 						Modifier
// 							.fillMaxSize()
// 							.padding(top = 5.dp),
// 						horizontalArrangement = Arrangement.Center,
// 					) {
// 						TextButton(
// 							enabled = !didExportFiles,
// 							onClick = {
// 								if (uiState.tunnels.isEmpty()) return@TextButton context.showToast(R.string.tunnel_required)
// 								showAuthPrompt = true
// 							},
// 						) {
// 							Text(stringResource(R.string.export_configs))
// 						}
// 					}
// 				}
// 			}
// 		}
	}
}
