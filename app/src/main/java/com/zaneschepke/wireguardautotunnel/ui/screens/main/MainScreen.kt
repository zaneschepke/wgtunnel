package com.zaneschepke.wireguardautotunnel.ui.screens.main

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.Route
import com.zaneschepke.wireguardautotunnel.ui.common.dialog.InfoDialog
import com.zaneschepke.wireguardautotunnel.ui.common.functions.rememberFileImportLauncherForResult
import com.zaneschepke.wireguardautotunnel.ui.common.navigation.LocalNavController
import com.zaneschepke.wireguardautotunnel.ui.screens.main.components.TunnelImportSheet
import com.zaneschepke.wireguardautotunnel.ui.screens.main.components.TunnelList
import com.zaneschepke.wireguardautotunnel.ui.screens.main.components.UrlImportDialog
import com.zaneschepke.wireguardautotunnel.ui.state.AppUiState
import com.zaneschepke.wireguardautotunnel.ui.state.AppViewState
import com.zaneschepke.wireguardautotunnel.util.Constants
import com.zaneschepke.wireguardautotunnel.util.StringValue
import com.zaneschepke.wireguardautotunnel.viewmodel.AppViewModel
import com.zaneschepke.wireguardautotunnel.viewmodel.event.AppEvent

@Composable
fun MainScreen(appUiState: AppUiState, appViewState: AppViewState, viewModel: AppViewModel) {
    val navController = LocalNavController.current
    val clipboard = LocalClipboardManager.current

    var showDeleteTunnelAlertDialog by remember { mutableStateOf(false) }
    var showUrlImportDialog by remember { mutableStateOf(false) }

    val tunnelFileImportResultLauncher =
        rememberFileImportLauncherForResult(
            onNoFileExplorer = {
                viewModel.handleEvent(
                    AppEvent.ShowMessage(
                        StringValue.StringResource(R.string.error_no_file_explorer)
                    )
                )
            },
            onData = { data -> viewModel.handleEvent(AppEvent.ImportTunnelFromFile(data)) },
        )

    val requestPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted
            ->
            if (!isGranted) {
                viewModel.handleEvent(
                    AppEvent.ShowMessage(
                        StringValue.StringResource(R.string.camera_permission_required)
                    )
                )
                return@rememberLauncherForActivityResult
            }
            navController.navigate(Route.Scanner)
        }

    if (showDeleteTunnelAlertDialog && appViewState.selectedTunnel != null) {
        InfoDialog(
            onDismiss = { showDeleteTunnelAlertDialog = false },
            onAttest = {
                appViewState.selectedTunnel.let { viewModel.handleEvent(AppEvent.DeleteTunnel(it)) }
                showDeleteTunnelAlertDialog = false
                viewModel.handleEvent(AppEvent.SetSelectedTunnel(null))
            },
            title = { Text(text = stringResource(R.string.delete_tunnel)) },
            body = { Text(text = stringResource(R.string.delete_tunnel_message)) },
            confirmText = { Text(text = stringResource(R.string.yes)) },
        )
    }

    TunnelImportSheet(
        appViewState.showBottomSheet,
        onDismiss = { viewModel.handleEvent(AppEvent.ToggleBottomSheet) },
        onFileClick = { tunnelFileImportResultLauncher.launch(Constants.ALLOWED_TV_FILE_TYPES) },
        onQrClick = { requestPermissionLauncher.launch(android.Manifest.permission.CAMERA) },
        onClipboardClick = {
            clipboard.getText()?.text?.let {
                viewModel.handleEvent(AppEvent.ImportTunnelFromClipboard(it))
            }
        },
        onManualImportClick = {
            navController.navigate(Route.Config(Constants.MANUAL_TUNNEL_CONFIG_ID))
        },
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
        selectedTunnel = appViewState.selectedTunnel,
        onSetSelectedTunnel = { viewModel.handleEvent(AppEvent.SetSelectedTunnel(it)) },
        onDeleteTunnel = {
            viewModel.handleEvent(AppEvent.SetSelectedTunnel(it))
            showDeleteTunnelAlertDialog = true
        },
        onToggleTunnel = { tunnel, checked ->
            if (checked) viewModel.handleEvent(AppEvent.StartTunnel(tunnel))
            else viewModel.handleEvent(AppEvent.StopTunnel(tunnel))
        },
        onExpandStats = { viewModel.handleEvent(AppEvent.ToggleTunnelStatsExpanded) },
        onCopyTunnel = {
            viewModel.handleEvent(AppEvent.CopyTunnel(it))
            viewModel.handleEvent(AppEvent.SetSelectedTunnel(null))
        },
        modifier = Modifier.fillMaxSize().padding(vertical = 24.dp).padding(horizontal = 12.dp),
        viewModel = viewModel,
    )
}
