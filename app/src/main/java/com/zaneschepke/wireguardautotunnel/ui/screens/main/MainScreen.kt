package com.zaneschepke.wireguardautotunnel.ui.screens.main

import android.annotation.SuppressLint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Circle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Info
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.wireguard.android.backend.Tunnel
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.WireGuardAutoTunnel
import com.zaneschepke.wireguardautotunnel.service.tunnel.HandshakeStatus
import com.zaneschepke.wireguardautotunnel.service.tunnel.model.TunnelConfig
import com.zaneschepke.wireguardautotunnel.ui.Routes
import com.zaneschepke.wireguardautotunnel.ui.common.RowListItem
import com.zaneschepke.wireguardautotunnel.ui.theme.brickRed
import com.zaneschepke.wireguardautotunnel.ui.theme.mint
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
    val isVisible = rememberSaveable { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }
    val tunnels by viewModel.tunnels.collectAsStateWithLifecycle(mutableListOf())
    val handshakeStatus by viewModel.handshakeStatus.collectAsStateWithLifecycle(HandshakeStatus.NOT_STARTED)
    val viewState = viewModel.viewState.collectAsStateWithLifecycle()
    var selectedTunnel by remember { mutableStateOf<TunnelConfig?>(null) }
    val state by viewModel.state.collectAsStateWithLifecycle(Tunnel.State.DOWN)
    val tunnelName by viewModel.tunnelName.collectAsStateWithLifecycle("")


    // Nested scroll for control FAB
    val nestedScrollConnection = remember {
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
            AnimatedVisibility(
                visible = isVisible.value,
                enter = slideInVertically(initialOffsetY = { it * 2 }),
                exit = slideOutVertically(targetOffsetY = { it * 2 }),
            ) {
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

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(nestedScrollConnection),
            ) {
                itemsIndexed(tunnels.toList()) { index, tunnel ->
                    val focusRequester = FocusRequester();
                    RowListItem(leadingIcon = Icons.Rounded.Circle,
                        leadingIconColor = if (tunnelName == tunnel.name) when (handshakeStatus) {
                            HandshakeStatus.HEALTHY -> mint
                            HandshakeStatus.UNHEALTHY -> brickRed
                            HandshakeStatus.NOT_STARTED -> Color.Gray
                            HandshakeStatus.NEVER_CONNECTED -> brickRed
                        } else Color.Gray,
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
                        onClick = {
                            if (!WireGuardAutoTunnel.isRunningOnAndroidTv(context)) {
                                navController.navigate("${Routes.Detail.name}/${tunnel.id}")
                            } else {
                                focusRequester.requestFocus()
                            }
                        },
                        rowButton = {
                            if (tunnel.id == selectedTunnel?.id) {
                                Row {
                                    IconButton(onClick = {
                                        navController.navigate("${Routes.Config.name}/${selectedTunnel?.id}")
                                    }) {
                                        Icon(Icons.Rounded.Edit, stringResource(id = R.string.edit))
                                    }
                                    IconButton(
                                        modifier = Modifier.focusable(),
                                        onClick = { viewModel.onDelete(tunnel) }) {
                                        Icon(
                                            Icons.Rounded.Delete,
                                            stringResource(id = R.string.delete)
                                        )
                                    }
                                }
                            } else {
                                if (WireGuardAutoTunnel.isRunningOnAndroidTv(context)) {
                                    Row {
                                        IconButton(
                                            modifier = Modifier.focusRequester(focusRequester),
                                            onClick = {
                                                navController.navigate("${Routes.Detail.name}/${tunnel.id}")
                                            }) {
                                            Icon(Icons.Rounded.Info, "Info")
                                        }
                                        IconButton(onClick = {
                                            if (state == Tunnel.State.UP && tunnel.name == tunnelName)
                                                scope.launch {
                                                    viewModel.showSnackBarMessage(
                                                        context.resources.getString(
                                                            R.string.turn_off_tunnel
                                                        )
                                                    )
                                                } else {
                                                navController.navigate("${Routes.Config.name}/${tunnel.id}")
                                            }
                                        }) {
                                            Icon(
                                                Icons.Rounded.Edit,
                                                stringResource(id = R.string.edit)
                                            )
                                        }
                                        IconButton(onClick = {
                                            if (state == Tunnel.State.UP && tunnel.name == tunnelName)
                                                scope.launch {
                                                    viewModel.showSnackBarMessage(
                                                        context.resources.getString(
                                                            R.string.turn_off_tunnel
                                                        )
                                                    )
                                                } else {
                                                viewModel.onDelete(tunnel)
                                            }
                                        }) {
                                            Icon(
                                                Icons.Rounded.Delete,
                                                stringResource(id = R.string.delete)
                                            )
                                        }
                                        Switch(
                                            checked = (state == Tunnel.State.UP && tunnel.name == tunnelName),
                                            onCheckedChange = { checked ->
                                                if (checked) viewModel.onTunnelStart(tunnel) else viewModel.onTunnelStop()
                                            }
                                        )
                                    }
                                } else {
                                    Switch(
                                        checked = (state == Tunnel.State.UP && tunnel.name == tunnelName),
                                        onCheckedChange = { checked ->
                                            if (checked) viewModel.onTunnelStart(tunnel) else viewModel.onTunnelStop()
                                        }
                                    )
                                }
                            }
                        })
                }
            }
        }
    }
}
