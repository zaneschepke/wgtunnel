package com.zaneschepke.wireguardautotunnel.ui.screens.main.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderZip
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.domain.enums.ConfigType
import com.zaneschepke.wireguardautotunnel.ui.common.functions.rememberFileExportLauncherForResult
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.components.AuthorizationPromptWrapper
import com.zaneschepke.wireguardautotunnel.ui.state.AppViewState
import com.zaneschepke.wireguardautotunnel.util.Constants
import com.zaneschepke.wireguardautotunnel.util.extensions.hasSAFSupport
import com.zaneschepke.wireguardautotunnel.viewmodel.AppViewModel
import com.zaneschepke.wireguardautotunnel.viewmodel.event.AppEvent
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportTunnelsBottomSheet(viewModel: AppViewModel, isRunningOnTv: Boolean) {
    val context = LocalContext.current

    var exportConfigType by remember { mutableStateOf(ConfigType.WG) }

    val selectedTunnelsExportLauncher =
        rememberFileExportLauncherForResult(
            mimeType = Constants.ZIP_FILE_MIME_TYPE,
            onResult = { file ->
                Timber.d("Export launcher result: file=$file")
                viewModel.handleEvent(AppEvent.ExportSelectedTunnels(exportConfigType, file))
            },
        )

    var showAuthPrompt by remember { mutableStateOf(false) }
    var isAuthorized by remember { mutableStateOf(false) }

    fun handleFileExport() {
        if (context.hasSAFSupport(Constants.ZIP_FILE_MIME_TYPE)) {
            selectedTunnelsExportLauncher.launch(Constants.DEFAULT_EXPORT_FILE_NAME)
        } else {
            viewModel.handleEvent(AppEvent.ExportSelectedTunnels(exportConfigType, null))
        }
    }

    if (showAuthPrompt) {
        AuthorizationPromptWrapper(
            onDismiss = { showAuthPrompt = false },
            onSuccess = {
                showAuthPrompt = false
                isAuthorized = true
            },
            viewModel = viewModel,
        )
    }

    ModalBottomSheet(
        containerColor = MaterialTheme.colorScheme.surface,
        onDismissRequest = {
            viewModel.handleEvent(AppEvent.SetBottomSheet(AppViewState.BottomSheet.NONE))
        },
    ) {
        Row(
            modifier =
                Modifier.fillMaxWidth()
                    .clickable {
                        exportConfigType = ConfigType.AMNEZIA
                        if (!isAuthorized && !isRunningOnTv) {
                            showAuthPrompt = true
                            return@clickable
                        }
                        handleFileExport()
                    }
                    .padding(10.dp)
        ) {
            Icon(
                Icons.Filled.FolderZip,
                contentDescription = stringResource(R.string.export_tunnels_amnezia),
                modifier = Modifier.padding(10.dp),
            )
            Text(
                stringResource(R.string.export_tunnels_amnezia),
                modifier = Modifier.padding(10.dp),
            )
        }
        HorizontalDivider()
        Row(
            modifier =
                Modifier.fillMaxWidth()
                    .clickable {
                        exportConfigType = ConfigType.WG
                        if (!isAuthorized && !isRunningOnTv) {
                            showAuthPrompt = true
                            return@clickable
                        }
                        handleFileExport()
                    }
                    .padding(10.dp)
        ) {
            Icon(
                Icons.Filled.FolderZip,
                contentDescription = stringResource(R.string.export_tunnels_wireguard),
                modifier = Modifier.padding(10.dp),
            )
            Text(
                stringResource(R.string.export_tunnels_wireguard),
                modifier = Modifier.padding(10.dp),
            )
        }
    }
}
