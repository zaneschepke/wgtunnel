package com.zaneschepke.wireguardautotunnel.ui.screens.main

import android.annotation.SuppressLint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Circle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.wireguard.android.backend.Tunnel
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.service.tunnel.HandshakeStatus
import com.zaneschepke.wireguardautotunnel.service.tunnel.model.TunnelConfig
import com.zaneschepke.wireguardautotunnel.ui.Routes
import com.zaneschepke.wireguardautotunnel.ui.common.RowListItem
import com.zaneschepke.wireguardautotunnel.ui.theme.brickRed
import com.zaneschepke.wireguardautotunnel.ui.theme.mint
import com.zaneschepke.wireguardautotunnel.ui.theme.pinkRed
import kotlinx.coroutines.launch

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel = hiltViewModel(), padding: PaddingValues,
    snackbarHostState: SnackbarHostState, navController: NavController
) {

    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }
    val tunnels by viewModel.tunnels.collectAsStateWithLifecycle(mutableListOf())
    val handshakeStatus by viewModel.handshakeStatus.collectAsStateWithLifecycle(HandshakeStatus.NOT_STARTED)
    val viewState = viewModel.viewState.collectAsStateWithLifecycle()
    var selectedTunnel by remember { mutableStateOf<TunnelConfig?>(null) }
    val state by viewModel.state.collectAsStateWithLifecycle(Tunnel.State.DOWN)
    val tunnelName by viewModel.tunnelName.collectAsStateWithLifecycle("")

    LaunchedEffect(viewState.value) {
        if (viewState.value.showSnackbarMessage) {
            val result = snackbarHostState.showSnackbar(
                message = viewState.value.snackbarMessage,
                actionLabel = viewState.value.snackbarActionText,
                duration = SnackbarDuration.Long,
            )
            when (result) {
                SnackbarResult.ActionPerformed -> viewState.value.onSnackbarActionClick
                SnackbarResult.Dismissed -> viewState.value.onSnackbarActionClick
            }
        }
    }

    val pickFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { file ->
        if (file != null) {
            viewModel.onTunnelFileSelected(file)
        }
    }

    Scaffold(
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures(onTap = {
                selectedTunnel = null
            })
        },
        floatingActionButtonPosition = FabPosition.End,
        floatingActionButton = {
            FloatingActionButton(
                modifier = Modifier.padding(bottom = 90.dp),
                onClick = {
                    showBottomSheet = true
                },
                containerColor = MaterialTheme.colorScheme.secondary,
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = stringResource(id = R.string.add_tunnel),
                    tint = Color.DarkGray,
                )
            }
        }
    ) {
        if (tunnels.isEmpty()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                Text(text = stringResource(R.string.no_tunnels), fontStyle = FontStyle.Italic)
            }
        }
        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = {
                    showBottomSheet = false
                },
                sheetState = sheetState
            ) {
                // Sheet content
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showBottomSheet = false
                            pickFileLauncher.launch("*/*")
                        }
                        .padding(10.dp)
                ) {
                    Icon(
                        Icons.Filled.FileOpen,
                        contentDescription = stringResource(id = R.string.open_file),
                        modifier = Modifier.padding(10.dp)
                    )
                    Text(
                        stringResource(id = R.string.add_from_file),
                        modifier = Modifier.padding(10.dp)
                    )
                }
                Divider()
                Row(modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        scope.launch {
                            showBottomSheet = false
                            viewModel.onTunnelQRSelected()
                        }
                    }
                    .padding(10.dp)
                ) {
                    Icon(
                        Icons.Filled.QrCode,
                        contentDescription = stringResource(id = R.string.qr_scan),
                        modifier = Modifier.padding(10.dp)
                    )
                    Text(
                        stringResource(id = R.string.add_from_qr),
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }
        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Top,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(tunnels.toList()) { tunnel ->
                    RowListItem(leadingIcon = Icons.Rounded.Circle,
                        leadingIconColor = when (handshakeStatus) {
                            HandshakeStatus.HEALTHY -> mint
                            HandshakeStatus.UNHEALTHY -> brickRed
                            HandshakeStatus.NOT_STARTED -> Color.Gray
                            HandshakeStatus.NEVER_CONNECTED -> brickRed
                        },
                        text = tunnel.name,
                        onHold = {
                            if (state == Tunnel.State.UP && tunnel.name == tunnelName) {
                                scope.launch {
                                    viewModel.showSnackBarMessage(context.resources.getString(R.string.turn_off_tunnel))
                                }
                                return@RowListItem
                            }
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            selectedTunnel = tunnel;
                        },
                        onClick = { navController.navigate("${Routes.Detail.name}/${tunnel.id}") },
                        rowButton = {
                            if (tunnel.id == selectedTunnel?.id) {
                                Row() {
                                    IconButton(onClick = {
                                        navController.navigate("${Routes.Config.name}/${selectedTunnel?.id}")
                                    }) {
                                        Icon(Icons.Rounded.Edit, stringResource(id = R.string.edit))
                                    }
                                    IconButton(onClick = { viewModel.onDelete(tunnel) }) {
                                        Icon(
                                            Icons.Rounded.Delete,
                                            stringResource(id = R.string.delete)
                                        )
                                    }
                                }
                            } else {
                                Switch(
                                    checked = (state == Tunnel.State.UP && tunnel.name == tunnelName),
                                    onCheckedChange = { checked ->
                                        if (checked) viewModel.onTunnelStart(tunnel) else viewModel.onTunnelStop()
                                    }
                                )
                            }
                        })
                }
            }
        }
    }
}
