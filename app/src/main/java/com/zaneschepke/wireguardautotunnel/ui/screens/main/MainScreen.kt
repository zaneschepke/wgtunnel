package com.zaneschepke.wireguardautotunnel.ui.screens.main

import android.annotation.SuppressLint
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity.RESULT_OK
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.overscroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.data.domain.TunnelConfig
import com.zaneschepke.wireguardautotunnel.ui.AppUiState
import com.zaneschepke.wireguardautotunnel.ui.Route
import com.zaneschepke.wireguardautotunnel.ui.common.NestedScrollListener
import com.zaneschepke.wireguardautotunnel.ui.common.dialog.InfoDialog
import com.zaneschepke.wireguardautotunnel.ui.common.functions.rememberFileImportLauncherForResult
import com.zaneschepke.wireguardautotunnel.ui.common.navigation.LocalNavController
import com.zaneschepke.wireguardautotunnel.ui.common.snackbar.SnackbarController
import com.zaneschepke.wireguardautotunnel.ui.screens.main.components.AutoTunnelRowItem
import com.zaneschepke.wireguardautotunnel.ui.screens.main.components.GettingStartedLabel
import com.zaneschepke.wireguardautotunnel.ui.screens.main.components.ScrollDismissFab
import com.zaneschepke.wireguardautotunnel.ui.screens.main.components.TunnelImportSheet
import com.zaneschepke.wireguardautotunnel.ui.screens.main.components.TunnelRowItem
import com.zaneschepke.wireguardautotunnel.ui.screens.main.components.VpnDeniedDialog
import com.zaneschepke.wireguardautotunnel.util.Constants
import com.zaneschepke.wireguardautotunnel.util.extensions.isRunningOnTv
import com.zaneschepke.wireguardautotunnel.util.extensions.openWebUrl
import com.zaneschepke.wireguardautotunnel.util.extensions.startTunnelBackground
import kotlinx.coroutines.delay

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainScreen(viewModel: MainViewModel = hiltViewModel(), uiState: AppUiState, focusRequester: FocusRequester) {
	val context = LocalContext.current
	val navController = LocalNavController.current
	val snackbar = SnackbarController.current

	var showBottomSheet by remember { mutableStateOf(false) }
	var showVpnPermissionDialog by remember { mutableStateOf(false) }
	var isFabVisible by rememberSaveable { mutableStateOf(true) }
	var showDeleteTunnelAlertDialog by remember { mutableStateOf(false) }
	var selectedTunnel by remember { mutableStateOf<TunnelConfig?>(null) }

	val nestedScrollConnection = remember {
		NestedScrollListener({ isFabVisible = false }, { isFabVisible = true })
	}

	val vpnActivityResultState =
		rememberLauncherForActivityResult(
			ActivityResultContracts.StartActivityForResult(),
			onResult = {
				if (it.resultCode != RESULT_OK) showVpnPermissionDialog = true
			},
		)

	LaunchedEffect(Unit) {
		if (context.isRunningOnTv()) {
			delay(Constants.FOCUS_REQUEST_DELAY)
			runCatching {
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
		val intent = if (uiState.settings.isKernelEnabled) null else VpnService.prepare(context)
		if (intent != null) return vpnActivityResultState.launch(intent)
		if (!checked) viewModel.onTunnelStop(tunnel).also { return }
		if (uiState.settings.isKernelEnabled) {
			context.startTunnelBackground(tunnel.id)
		} else {
			viewModel.onTunnelStart(tunnel)
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
			if (uiState.tunnels.isEmpty()) return@pointerInput
			detectTapGestures(
				onTap = {
					selectedTunnel = null
				},
			)
		},
		floatingActionButtonPosition = FabPosition.End,
		floatingActionButton = {
			ScrollDismissFab({
				val icon = Icons.Filled.Add
				Icon(
					imageVector = icon,
					contentDescription = icon.name,
					tint = MaterialTheme.colorScheme.onPrimary,
				)
			}, focusRequester, isVisible = isFabVisible, onClick = {
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
					Route.Config(Constants.MANUAL_TUNNEL_CONFIG_ID),
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
			if (uiState.settings.isAutoTunnelEnabled) {
				item {
					AutoTunnelRowItem(uiState.settings, { viewModel.onToggleAutoTunnelingPause() }, focusRequester)
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
				val expanded = uiState.generalState.isTunnelStatsExpanded
				TunnelRowItem(
					isActive,
					expanded,
					selectedTunnel?.id == tunnel.id,
					tunnel,
					vpnState = uiState.vpnState,
					{ selectedTunnel = tunnel },
					{ viewModel.onExpandedChanged(!expanded) },
					onDelete = { showDeleteTunnelAlertDialog = true },
					onCopy = { viewModel.onCopyTunnel(tunnel) },
					onSwitchClick = { onTunnelToggle(it, tunnel) },
					focusRequester = focusRequester,
				)
			}
		}
	}
}
