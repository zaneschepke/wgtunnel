package com.zaneschepke.wireguardautotunnel.ui.screens.main

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.domain.entity.TunnelConf
import com.zaneschepke.wireguardautotunnel.ui.Route
import com.zaneschepke.wireguardautotunnel.ui.common.NestedScrollListener
import com.zaneschepke.wireguardautotunnel.ui.common.dialog.InfoDialog
import com.zaneschepke.wireguardautotunnel.ui.common.functions.rememberFileImportLauncherForResult
import com.zaneschepke.wireguardautotunnel.ui.common.navigation.LocalNavController
import com.zaneschepke.wireguardautotunnel.ui.common.permission.vpn.withVpnPermission
import com.zaneschepke.wireguardautotunnel.ui.common.permission.withIgnoreBatteryOpt
import com.zaneschepke.wireguardautotunnel.ui.common.snackbar.SnackbarController
import com.zaneschepke.wireguardautotunnel.ui.screens.main.components.AddTunnelFab
import com.zaneschepke.wireguardautotunnel.ui.screens.main.components.TunnelImportSheet
import com.zaneschepke.wireguardautotunnel.ui.screens.main.components.TunnelList
import com.zaneschepke.wireguardautotunnel.ui.screens.main.components.UrlImportDialog
import com.zaneschepke.wireguardautotunnel.ui.state.AppUiState
import com.zaneschepke.wireguardautotunnel.util.Constants
import com.zaneschepke.wireguardautotunnel.util.extensions.isRunningOnTv
import com.zaneschepke.wireguardautotunnel.util.extensions.scaledHeight
import com.zaneschepke.wireguardautotunnel.viewmodel.AppViewModel
import com.zaneschepke.wireguardautotunnel.viewmodel.event.AppEvent

@Composable
fun MainScreen(appUiState: AppUiState, viewModel: AppViewModel) {
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

	val startAutoTunnel = withVpnPermission<Unit> { viewModel.handleEvent(AppEvent.ToggleAutoTunnel) }
	val startTunnel = withVpnPermission<TunnelConf> { viewModel.handleEvent(AppEvent.StartTunnel(it)) }
	val autoTunnelToggleBattery = withIgnoreBatteryOpt(appUiState.generalState.isBatteryOptimizationDisableShown) {
		if (!appUiState.generalState.isBatteryOptimizationDisableShown) viewModel.handleEvent(AppEvent.SetBatteryOptimizeDisableShown)
		if (appUiState.appSettings.isKernelEnabled) viewModel.handleEvent(AppEvent.ToggleAutoTunnel) else startAutoTunnel.invoke(Unit)
	}

	val tunnelFileImportResultLauncher = rememberFileImportLauncherForResult(
		onNoFileExplorer = { snackbar.showMessage(context.getString(R.string.error_no_file_explorer)) },
		onData = { data -> viewModel.handleEvent(AppEvent.ImportTunnelFromFile(data)) },
	)

	val requestPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
		if (!isGranted) return@rememberLauncherForActivityResult snackbar.showMessage("Camera permission required")
		navController.navigate(Route.Scanner)
	}

	val nestedScrollConnection = remember {
		NestedScrollListener({ isFabVisible = false }, { isFabVisible = true })
	}

	if (showDeleteTunnelAlertDialog && selectedTunnel != null) {
		InfoDialog(
			onDismiss = { showDeleteTunnelAlertDialog = false },
			onAttest = {
				selectedTunnel?.let { viewModel.handleEvent(AppEvent.DeleteTunnel(it)) }
				showDeleteTunnelAlertDialog = false
				selectedTunnel = null
			},
			title = { Text(text = stringResource(R.string.delete_tunnel)) },
			body = { Text(text = stringResource(R.string.delete_tunnel_message)) },
			confirmText = { Text(text = stringResource(R.string.yes)) },
		)
	}

	Scaffold(
		floatingActionButtonPosition = FabPosition.End,
		floatingActionButton = {
			if (!isRunningOnTv) {
				AddTunnelFab(
					isVisible = isFabVisible,
					onClick = { showBottomSheet = true },
				)
			}
		},
		topBar = {
			if (isRunningOnTv) {
				AddTunnelFab(
					isVisible = isFabVisible,
					isTv = true,
					onClick = { showBottomSheet = true },
				)
			}
		},
	) { padding ->
		TunnelImportSheet(
			showBottomSheet,
			onDismiss = { showBottomSheet = false },
			onFileClick = { tunnelFileImportResultLauncher.launch(Constants.ALLOWED_TV_FILE_TYPES) },
			onQrClick = { requestPermissionLauncher.launch(android.Manifest.permission.CAMERA) },
			onClipboardClick = { clipboard.getText()?.text?.let { viewModel.handleEvent(AppEvent.ImportTunnelFromClipboard(it)) } },
			onManualImportClick = { navController.navigate(Route.Config(Constants.MANUAL_TUNNEL_CONFIG_ID)) },
			onUrlClick = { showUrlImportDialog = true },
		)

		if (showUrlImportDialog) {
			UrlImportDialog(
				onDismiss = { showUrlImportDialog = false },
				onConfirm = { url ->
					viewModel.handleEvent(AppEvent.ImportTunnelFromUrl(url))
					showUrlImportDialog = false
				},
			)
		}

		TunnelList(
			appUiState = appUiState,
			activeTunnels = appUiState.activeTunnels,
			selectedTunnel = selectedTunnel,
			onTunnelSelected = { selectedTunnel = it },
			onDeleteTunnel = {
				selectedTunnel = it
				showDeleteTunnelAlertDialog = true
			},
			onToggleAutoTunnel = { autoTunnelToggleBattery.invoke() },
			onToggleTunnel = { tunnel, checked ->
				if (checked) startTunnel(tunnel) else viewModel.handleEvent(AppEvent.StopTunnel(tunnel))
			},
			onExpandStats = { viewModel.handleEvent(AppEvent.ToggleTunnelStatsExpanded) },
			onCopyTunnel = { viewModel.handleEvent(AppEvent.CopyTunnel(it)) },
			nestedScrollConnection,
			modifier = Modifier
				.fillMaxSize()
				.padding(padding)
				.padding(top = 24.dp.scaledHeight()),
		)
	}
}
