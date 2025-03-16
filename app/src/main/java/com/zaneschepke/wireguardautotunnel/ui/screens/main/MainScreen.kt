package com.zaneschepke.wireguardautotunnel.ui.screens.main

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.overscroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.domain.entity.TunnelConf
import com.zaneschepke.wireguardautotunnel.domain.state.TunnelState
import com.zaneschepke.wireguardautotunnel.ui.state.AppUiState
import com.zaneschepke.wireguardautotunnel.ui.Route
import com.zaneschepke.wireguardautotunnel.ui.common.NestedScrollListener
import com.zaneschepke.wireguardautotunnel.ui.common.dialog.InfoDialog
import com.zaneschepke.wireguardautotunnel.ui.common.functions.rememberFileImportLauncherForResult
import com.zaneschepke.wireguardautotunnel.ui.common.navigation.LocalNavController
import com.zaneschepke.wireguardautotunnel.ui.common.navigation.TopNavBar
import com.zaneschepke.wireguardautotunnel.ui.common.permission.vpn.withVpnPermission
import com.zaneschepke.wireguardautotunnel.ui.common.permission.withIgnoreBatteryOpt
import com.zaneschepke.wireguardautotunnel.ui.common.snackbar.SnackbarController
import com.zaneschepke.wireguardautotunnel.ui.screens.main.components.AutoTunnelRowItem
import com.zaneschepke.wireguardautotunnel.ui.screens.main.components.GettingStartedLabel
import com.zaneschepke.wireguardautotunnel.ui.screens.main.components.ScrollDismissFab
import com.zaneschepke.wireguardautotunnel.ui.screens.main.components.TunnelImportSheet
import com.zaneschepke.wireguardautotunnel.ui.screens.main.components.TunnelRowItem
import com.zaneschepke.wireguardautotunnel.ui.screens.main.components.UrlImportDialog
import com.zaneschepke.wireguardautotunnel.util.Constants
import com.zaneschepke.wireguardautotunnel.util.extensions.isRunningOnTv
import com.zaneschepke.wireguardautotunnel.util.extensions.openWebUrl
import com.zaneschepke.wireguardautotunnel.util.extensions.scaledHeight
import com.zaneschepke.wireguardautotunnel.viewmodel.MainViewModel
import java.text.Collator
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainScreen(viewModel: MainViewModel = hiltViewModel(), uiState: AppUiState) {
	val context = LocalContext.current
	val navController = LocalNavController.current
	val clipboard = LocalClipboardManager.current
	val snackbar = SnackbarController.current

	var showBottomSheet by remember { mutableStateOf(false) }
	var isFabVisible by rememberSaveable { mutableStateOf(true) }
	var showDeleteTunnelAlertDialog by remember { mutableStateOf(false) }
	var selectedTunnel by remember { mutableStateOf<TunnelConf?>(null) }
	var showUrlImportDialog by remember { mutableStateOf(false) }
	val isRunningOnTv = remember { context.isRunningOnTv() }

	val activeTunnels by viewModel.tunnelManager.activeTunnels.collectAsStateWithLifecycle(emptyMap())

	val collator = Collator.getInstance(Locale.getDefault())

	val sortedTunnels = remember(uiState.tunnels) {
		uiState.tunnels.sortedWith(compareBy(collator) { it.tunName })
	}

	val startAutoTunnel = withVpnPermission<Unit> { viewModel.onToggleAutoTunnel() }
	val startTunnel = withVpnPermission<TunnelConf> {
		viewModel.onTunnelStart(it)
	}

	val autoTunnelToggleBattery = withIgnoreBatteryOpt(uiState.generalState.isBatteryOptimizationDisableShown) {
		if (!uiState.generalState.isBatteryOptimizationDisableShown) viewModel.setBatteryOptimizeDisableShown()
		if (uiState.appSettings.isKernelEnabled) {
			viewModel.onToggleAutoTunnel()
		} else {
			startAutoTunnel.invoke(Unit)
		}
	}

	val nestedScrollConnection = remember {
		NestedScrollListener({ isFabVisible = false }, { isFabVisible = true })
	}

	val tunnelFileImportResultLauncher = rememberFileImportLauncherForResult(onNoFileExplorer = {
		snackbar.showMessage(
			context.getString(R.string.error_no_file_explorer),
		)
	}, onData = { data ->
		viewModel.onTunnelFileSelected(data, context)
	})

	val requestPermissionLauncher = rememberLauncherForActivityResult(
		ActivityResultContracts.RequestPermission(),
	) { isGranted ->
		if (!isGranted) return@rememberLauncherForActivityResult snackbar.showMessage("Camera permission required")
		navController.navigate(Route.Scanner)
	}

	if (showDeleteTunnelAlertDialog) {
		InfoDialog(
			onDismiss = { showDeleteTunnelAlertDialog = false },
			onAttest = {
				selectedTunnel?.let { viewModel.onDelete(it) }
				showDeleteTunnelAlertDialog = false
				selectedTunnel = null
			},
			title = { Text(text = stringResource(R.string.delete_tunnel)) },
			body = { Text(text = stringResource(R.string.delete_tunnel_message)) },
			confirmText = { Text(text = stringResource(R.string.yes)) },
		)
	}

	fun onTunnelToggle(checked: Boolean, tunnel: TunnelConf) {
		if (!checked) return viewModel.onTunnelStop(tunnel).let { }
		if (uiState.appSettings.isKernelEnabled) viewModel.onTunnelStart(tunnel) else startTunnel(tunnel)
	}

	Scaffold(
		modifier =
		Modifier.pointerInput(Unit) {
			if (uiState.tunnels.isEmpty()) return@pointerInput
			detectTapGestures(
				onTap = {
					selectedTunnel = null
				},
			)
		},
		floatingActionButtonPosition = FabPosition.End,
		floatingActionButton = {
			if (!isRunningOnTv) {
				ScrollDismissFab({
					val icon = Icons.Filled.Add
					Icon(
						imageVector = icon,
						contentDescription = icon.name,
						tint = MaterialTheme.colorScheme.onPrimary,
					)
				}, isVisible = isFabVisible, onClick = {
					showBottomSheet = true
				})
			}
		},
		topBar = {
			if (isRunningOnTv) {
				TopNavBar(
					showBack = false,
					title = stringResource(R.string.app_name),
					trailing = {
						IconButton(onClick = {
							showBottomSheet = true
						}) {
							val icon = Icons.Outlined.Add
							Icon(
								imageVector = icon,
								contentDescription = icon.name,
							)
						}
					},
				)
			}
		},
	) { padding ->
		TunnelImportSheet(
			showBottomSheet,
			onDismiss = { showBottomSheet = false },
			onFileClick = { tunnelFileImportResultLauncher.launch(Constants.ALLOWED_TV_FILE_TYPES) },
			onQrClick = { requestPermissionLauncher.launch(android.Manifest.permission.CAMERA) },
			onClipboardClick = {
				clipboard.getText()?.text?.let {
					viewModel.onClipboardImport(it)
				}
			},
			onManualImportClick = {
				navController.navigate(
					Route.Config(Constants.MANUAL_TUNNEL_CONFIG_ID),
				)
			},
			onUrlClick = { showUrlImportDialog = true }
		)

		if (showUrlImportDialog) {
			UrlImportDialog(
				onDismiss = { showUrlImportDialog = false },
				onConfirm = { url ->
					viewModel.onUrlImport(url)
					showUrlImportDialog = false
				}
			)
		}

		LazyColumn(
			horizontalAlignment = Alignment.Start,
			verticalArrangement = Arrangement.spacedBy(5.dp, Alignment.Top),
			modifier =
			Modifier
				.fillMaxSize().padding(padding).padding(top = 24.dp.scaledHeight())
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
			} else {
				item {
					AutoTunnelRowItem(uiState) {
						autoTunnelToggleBattery.invoke()
					}
				}
			}
			items(
				sortedTunnels,
				key = { tunnel -> tunnel.id },
			) { tunnel ->
				val expanded = uiState.generalState.isTunnelStatsExpanded
				val tunnelState = activeTunnels.getOrDefault(tunnel.id, TunnelState())
				TunnelRowItem(
					tunnelState.state.isUp(),
					expanded,
					selectedTunnel?.id == tunnel.id,
					tunnel,
					tunnelState = tunnelState,
					{ selectedTunnel = tunnel },
					{ viewModel.onExpandedChanged(!expanded) },
					onDelete = { showDeleteTunnelAlertDialog = true },
					onCopy = { viewModel.onCopyTunnel(tunnel) },
					onSwitchClick = { onTunnelToggle(it, tunnel) },
				)
			}
		}
	}
}
