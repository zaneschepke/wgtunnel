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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.Route
import com.zaneschepke.wireguardautotunnel.ui.common.dialog.InfoDialog
import com.zaneschepke.wireguardautotunnel.ui.common.functions.rememberFileImportLauncherForResult
import com.zaneschepke.wireguardautotunnel.ui.navigation.LocalNavController
import com.zaneschepke.wireguardautotunnel.ui.screens.main.components.ExportTunnelsBottomSheet
import com.zaneschepke.wireguardautotunnel.ui.screens.main.components.TunnelImportSheet
import com.zaneschepke.wireguardautotunnel.ui.screens.main.components.TunnelList
import com.zaneschepke.wireguardautotunnel.ui.screens.main.components.UrlImportDialog
import com.zaneschepke.wireguardautotunnel.ui.state.AppUiState
import com.zaneschepke.wireguardautotunnel.ui.state.AppViewState
import com.zaneschepke.wireguardautotunnel.util.Constants
import com.zaneschepke.wireguardautotunnel.util.StringValue
import com.zaneschepke.wireguardautotunnel.util.extensions.isRunningOnTv
import com.zaneschepke.wireguardautotunnel.viewmodel.AppViewModel
import com.zaneschepke.wireguardautotunnel.viewmodel.event.AppEvent

@Composable
fun MainScreen(appUiState: AppUiState, appViewState: AppViewState, viewModel: AppViewModel) {
    val navController = LocalNavController.current
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    var showUrlImportDialog by remember { mutableStateOf(false) }
    val isRunningOnTv = remember { context.isRunningOnTv() }

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

    if (appViewState.showModal == AppViewState.ModalType.DELETE) {
        InfoDialog(
            onDismiss = {
                viewModel.handleEvent(AppEvent.SetShowModal(AppViewState.ModalType.NONE))
            },
            onAttest = {
                viewModel.handleEvent(AppEvent.DeleteSelectedTunnels)
                viewModel.handleEvent(AppEvent.SetShowModal(AppViewState.ModalType.NONE))
            },
            title = { Text(text = stringResource(R.string.delete_tunnel)) },
            body = { Text(text = stringResource(R.string.delete_tunnel_message)) },
            confirmText = { Text(text = stringResource(R.string.yes)) },
        )
    }

    when (appViewState.bottomSheet) {
        AppViewState.BottomSheet.EXPORT_TUNNELS -> {
            ExportTunnelsBottomSheet(viewModel, isRunningOnTv)
        }
        AppViewState.BottomSheet.IMPORT_TUNNELS -> {
            TunnelImportSheet(
                onDismiss = {
                    viewModel.handleEvent(AppEvent.SetBottomSheet(AppViewState.BottomSheet.NONE))
                },
                onFileClick = {
                    tunnelFileImportResultLauncher.launch(Constants.ALLOWED_TV_FILE_TYPES)
                },
                onQrClick = {
                    requestPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                },
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
        }
        else -> Unit
    }

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
        selectedTunnels = appViewState.selectedTunnels,
        onToggleTunnel = { tunnel, checked ->
            if (checked) viewModel.handleEvent(AppEvent.StartTunnel(tunnel))
            else viewModel.handleEvent(AppEvent.StopTunnel(tunnel))
        },
        modifier = Modifier.fillMaxSize().padding(vertical = 24.dp).padding(horizontal = 12.dp),
        viewModel = viewModel,
    )
}
