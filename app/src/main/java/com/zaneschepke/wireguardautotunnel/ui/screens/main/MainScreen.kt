package com.zaneschepke.wireguardautotunnel.ui.screens.main

import android.annotation.SuppressLint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity.RESULT_OK
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.overscroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Circle
import androidx.compose.material.icons.rounded.CopyAll
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.wireguard.android.backend.GoBackend
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.data.domain.TunnelConfig
import com.zaneschepke.wireguardautotunnel.service.tunnel.HandshakeStatus
import com.zaneschepke.wireguardautotunnel.service.tunnel.TunnelState
import com.zaneschepke.wireguardautotunnel.ui.AppUiState
import com.zaneschepke.wireguardautotunnel.ui.Screens
import com.zaneschepke.wireguardautotunnel.ui.common.RowListItem
import com.zaneschepke.wireguardautotunnel.ui.common.dialog.InfoDialog
import com.zaneschepke.wireguardautotunnel.ui.common.functions.rememberFileImportLauncherForResult
import com.zaneschepke.wireguardautotunnel.ui.common.snackbar.SnackbarController
import com.zaneschepke.wireguardautotunnel.ui.screens.main.components.GettingStartedLabel
import com.zaneschepke.wireguardautotunnel.ui.screens.main.components.ScrollDismissFab
import com.zaneschepke.wireguardautotunnel.ui.screens.main.components.TunnelImportSheet
import com.zaneschepke.wireguardautotunnel.ui.screens.main.components.VpnDeniedDialog
import com.zaneschepke.wireguardautotunnel.ui.theme.corn
import com.zaneschepke.wireguardautotunnel.ui.theme.mint
import com.zaneschepke.wireguardautotunnel.util.Constants
import com.zaneschepke.wireguardautotunnel.util.extensions.handshakeStatus
import com.zaneschepke.wireguardautotunnel.util.extensions.isRunningOnTv
import com.zaneschepke.wireguardautotunnel.util.extensions.mapPeerStats
import com.zaneschepke.wireguardautotunnel.util.extensions.openWebUrl
import com.zaneschepke.wireguardautotunnel.util.extensions.startTunnelBackground
import kotlinx.coroutines.delay
import timber.log.Timber

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainScreen(viewModel: MainViewModel = hiltViewModel(), uiState: AppUiState, focusRequester: FocusRequester, navController: NavController) {
	val haptic = LocalHapticFeedback.current
	val context = LocalContext.current
	val snackbar = SnackbarController.current

	var showBottomSheet by remember { mutableStateOf(false) }
	var showVpnPermissionDialog by remember { mutableStateOf(false) }
	val isVisible = rememberSaveable { mutableStateOf(true) }
	var showDeleteTunnelAlertDialog by remember { mutableStateOf(false) }
	var selectedTunnel by remember { mutableStateOf<TunnelConfig?>(null) }

	val nestedScrollConnection =
		remember {
			object : NestedScrollConnection {
				override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
					// Hide FAB
					if (available.y < -1) {
						isVisible.value = false
					}
					// Show FAB
					if (available.y > 1) {
						isVisible.value = true
					}
					return Offset.Zero
				}
			}
		}

	val vpnActivityResultState =
		rememberLauncherForActivityResult(
			ActivityResultContracts.StartActivityForResult(),
			onResult = {
				val accepted = (it.resultCode == RESULT_OK)
				if (accepted) {
					Timber.d("VPN permission granted")
				} else {
					showVpnPermissionDialog = true
				}
			},
		)

	LaunchedEffect(Unit) {
		if (context.isRunningOnTv()) {
			delay(Constants.FOCUS_REQUEST_DELAY)
			kotlin.runCatching {
				focusRequester.requestFocus()
			}.onFailure {
				delay(Constants.FOCUS_REQUEST_DELAY)
				focusRequester.requestFocus()
			}
		}
	}

	val tunnelFileImportResultLauncher = rememberFileImportLauncherForResult(onNoFileExplorer = {
		snackbar.showMessage(
			context.getString(R.string.error_no_file_explorer),
		)
	}, onData = { data ->
		viewModel.onTunnelFileSelected(data, context)
	})

	val scanLauncher =
		rememberLauncherForActivityResult(
			contract = ScanContract(),
			onResult = {
				if (it.contents != null) {
					viewModel.onTunnelQrResult(it.contents)
				}
			},
		)

	VpnDeniedDialog(showVpnPermissionDialog, onDismiss = { showVpnPermissionDialog = false })

	if (showDeleteTunnelAlertDialog) {
		InfoDialog(
			onDismiss = { showDeleteTunnelAlertDialog = false },
			onAttest = {
				selectedTunnel?.let { viewModel.onDelete(it, context) }
				showDeleteTunnelAlertDialog = false
				selectedTunnel = null
			},
			title = { Text(text = stringResource(R.string.delete_tunnel)) },
			body = { Text(text = stringResource(R.string.delete_tunnel_message)) },
			confirmText = { Text(text = stringResource(R.string.yes)) },
		)
	}

	fun onTunnelToggle(checked: Boolean, tunnel: TunnelConfig) {
		if (checked) {
			if (uiState.settings.isKernelEnabled) {
				context.startTunnelBackground(tunnel.id)
			} else {
				viewModel.onTunnelStart(tunnel)
			}
		} else {
			viewModel.onTunnelStop(
				tunnel,
			)
		}
	}

	fun launchQrScanner() {
		val scanOptions = ScanOptions()
		scanOptions.setDesiredBarcodeFormats(ScanOptions.QR_CODE)
		scanOptions.setOrientationLocked(true)
		scanOptions.setPrompt(
			context.getString(R.string.scanning_qr),
		)
		scanOptions.setBeepEnabled(false)
		scanLauncher.launch(scanOptions)
	}

	Scaffold(
		modifier =
		Modifier.pointerInput(Unit) {
			if (uiState.tunnels.isNotEmpty()) {
				detectTapGestures(
					onTap = {
						selectedTunnel = null
					},
				)
			}
		},
		floatingActionButtonPosition = FabPosition.End,
		floatingActionButton = {
			ScrollDismissFab(icon = {
				val icon = Icons.Filled.Add
				Icon(
					imageVector = icon,
					contentDescription = icon.name,
					tint = MaterialTheme.colorScheme.onPrimary,
				)
			}, focusRequester, isVisible = isVisible.value, onClick = {
				showBottomSheet = true
			})
		},
	) {
		TunnelImportSheet(
			showBottomSheet,
			onDismiss = { showBottomSheet = false },
			onFileClick = { tunnelFileImportResultLauncher.launch(Constants.ALLOWED_FILE_TYPES) },
			onQrClick = { launchQrScanner() },
			onManualImportClick = {
				navController.navigate(
					Screens.Config(Constants.MANUAL_TUNNEL_CONFIG_ID),
				)
			},
		)
		LazyColumn(
			horizontalAlignment = Alignment.Start,
			verticalArrangement = Arrangement.Top,
			modifier =
			Modifier
				.fillMaxSize()
				.overscroll(ScrollableDefaults.overscrollEffect())
				.nestedScroll(nestedScrollConnection),
			state = rememberLazyListState(0, uiState.tunnels.count()),
			userScrollEnabled = true,
			reverseLayout = false,
			flingBehavior = ScrollableDefaults.flingBehavior(),
		) {
			if (uiState.tunnels.isEmpty()) {
				item {
					GettingStartedLabel(onClick = { context.openWebUrl(it) })
				}
			}
			item {
				if (uiState.settings.isAutoTunnelEnabled) {
					val itemFocusRequester = remember { FocusRequester() }
					val autoTunnelingLabel =
						buildAnnotatedString {
							append(stringResource(id = R.string.auto_tunneling))
							append(": ")
							if (uiState.settings.isAutoTunnelPaused) {
								append(
									stringResource(id = R.string.paused),
								)
							} else {
								append(
									stringResource(id = R.string.active),
								)
							}
						}
					RowListItem(
						icon = {
							val icon = Icons.Rounded.Bolt
							Icon(
								icon,
								icon.name,
								modifier =
								Modifier
									.padding(end = 8.5.dp)
									.size(25.dp),
								tint =
								if (uiState.settings.isAutoTunnelPaused) {
									Color.Gray
								} else {
									mint
								},
							)
						},
						text = autoTunnelingLabel.text,
						rowButton = {
							if (uiState.settings.isAutoTunnelPaused) {
								TextButton(
									modifier = Modifier.focusRequester(itemFocusRequester),
									onClick = { viewModel.resumeAutoTunneling() },
								) {
									Text(stringResource(id = R.string.resume))
								}
							} else {
								TextButton(
									modifier = Modifier.focusRequester(itemFocusRequester),
									onClick = { viewModel.pauseAutoTunneling() },
								) {
									Text(stringResource(id = R.string.pause))
								}
							}
						},
						onClick = {
							if (context.isRunningOnTv()) {
								itemFocusRequester.requestFocus()
							}
						},
						onHold = {},
						expanded = false,
						statistics = null,
						focusRequester = focusRequester,
					)
				}
			}
			items(
				uiState.tunnels,
				key = { tunnel -> tunnel.id },
			) { tunnel ->
				val isActive = uiState.tunnels.any {
					it.id == tunnel.id &&
						it.isActive
				}
				val leadingIconColor =
					(
						if (
							isActive && uiState.vpnState.statistics != null
						) {
							uiState.vpnState.statistics.mapPeerStats()
								.map { it.value?.handshakeStatus() }
								.let { statuses ->
									when {
										statuses.all { it == HandshakeStatus.HEALTHY } -> mint
										statuses.any { it == HandshakeStatus.STALE } -> corn
										statuses.all { it == HandshakeStatus.NOT_STARTED } ->
											Color.Gray

										else -> {
											Color.Gray
										}
									}
								}
						} else {
							Color.Gray
						}
						)
				val itemFocusRequester = remember { FocusRequester() }
				val expanded = remember { mutableStateOf(false) }
				RowListItem(
					icon = {
						val circleIcon = Icons.Rounded.Circle
						val icon =
							if (tunnel.isPrimaryTunnel) {
								Icons.Rounded.Star
							} else if (tunnel.isMobileDataTunnel) {
								Icons.Rounded.Smartphone
							} else {
								circleIcon
							}
						Icon(
							icon,
							icon.name,
							tint = leadingIconColor,
							modifier =
							Modifier
								.padding(
									end = if (icon == circleIcon) 12.5.dp else 10.dp,
									start = if (icon == circleIcon) 2.5.dp else 0.dp,
								)
								.size(if (icon == circleIcon) 15.dp else 20.dp),
						)
					},
					text = tunnel.name,
					onHold = {
						haptic.performHapticFeedback(HapticFeedbackType.LongPress)
						selectedTunnel = tunnel
					},
					onClick = {
						if (!context.isRunningOnTv()) {
							if (
								isActive
							) {
								expanded.value = !expanded.value
							}
						} else {
							selectedTunnel = tunnel
							itemFocusRequester.requestFocus()
						}
					},
					statistics = uiState.vpnState.statistics,
					expanded = expanded.value,
					focusRequester = focusRequester,
					rowButton = {
						if (
							tunnel.id == selectedTunnel?.id &&
							!context.isRunningOnTv()
						) {
							Row {
								IconButton(
									onClick = {
										selectedTunnel?.let {
											navController.navigate(
												Screens.Option(it.id),
											)
										}
									},
								) {
									val icon = Icons.Rounded.Settings
									Icon(
										icon,
										icon.name,
									)
								}
								IconButton(
									modifier = Modifier.focusable(),
									onClick = { viewModel.onCopyTunnel(selectedTunnel) },
								) {
									val icon = Icons.Rounded.CopyAll
									Icon(icon, icon.name)
								}
								IconButton(
									enabled = !isActive,
									modifier = Modifier.focusable(),
									onClick = { showDeleteTunnelAlertDialog = true },
								) {
									val icon = Icons.Rounded.Delete
									Icon(icon, icon.name)
								}
							}
						} else {
							if (!isActive) expanded.value = false
							@Composable
							fun TunnelSwitch() = Switch(
								modifier = Modifier.focusRequester(itemFocusRequester),
								checked = isActive,
								onCheckedChange = { checked ->
									if (!checked) expanded.value = false
									val intent = if (uiState.settings.isKernelEnabled) null else GoBackend.VpnService.prepare(context)
									if (intent != null) return@Switch vpnActivityResultState.launch(intent)
									onTunnelToggle(checked, tunnel)
								},
							)
							if (context.isRunningOnTv()) {
								Row {
									IconButton(
										onClick = {
											selectedTunnel = tunnel
											selectedTunnel?.let {
												navController.navigate(
													Screens.Option(it.id),
												)
											}
										},
									) {
										val icon = Icons.Rounded.Settings
										Icon(
											icon,
											icon.name,
										)
									}
									IconButton(
										modifier = Modifier.focusRequester(focusRequester),
										onClick = {
											if (
												uiState.vpnState.status == TunnelState.UP &&
												(uiState.vpnState.tunnelConfig?.name == tunnel.name)
											) {
												expanded.value = !expanded.value
											} else {
												snackbar.showMessage(
													context.getString(R.string.turn_on_tunnel),
												)
											}
										},
									) {
										val icon = Icons.Rounded.Info
										Icon(icon, icon.name)
									}
									IconButton(
										onClick = { viewModel.onCopyTunnel(tunnel) },
									) {
										val icon = Icons.Rounded.CopyAll
										Icon(icon, icon.name)
									}
									IconButton(
										onClick = {
											if (
												uiState.vpnState.status == TunnelState.UP &&
												tunnel.name == uiState.vpnState.tunnelConfig?.name
											) {
												snackbar.showMessage(
													context.getString(R.string.turn_off_tunnel),
												)
											} else {
												selectedTunnel = tunnel
												showDeleteTunnelAlertDialog = true
											}
										},
									) {
										val icon = Icons.Rounded.Delete
										Icon(
											icon,
											icon.name,
										)
									}
									TunnelSwitch()
								}
							} else {
								TunnelSwitch()
							}
						}
					},
				)
			}
		}
	}
}
