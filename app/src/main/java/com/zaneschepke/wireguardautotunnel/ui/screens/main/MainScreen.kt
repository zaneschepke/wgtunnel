package com.zaneschepke.wireguardautotunnel.ui.screens.main

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import com.wireguard.android.backend.Tunnel
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.service.tunnel.model.TunnelConfig
import com.zaneschepke.wireguardautotunnel.ui.common.RowListItem
import kotlinx.coroutines.launch

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel = hiltViewModel(), padding : PaddingValues,
               snackbarHostState : SnackbarHostState) {

    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val tunnels by viewModel.tunnels.collectAsStateWithLifecycle(mutableListOf())
    val viewState = viewModel.viewState.collectAsStateWithLifecycle()
    var showAlertDialog by remember { mutableStateOf(false) }
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
                    pickFileLauncher.launch("*/*")
                },
                containerColor = MaterialTheme.colorScheme.secondary,
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "Add Tunnel",
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
        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Top,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(tunnels.toList()) { tunnel ->
                    RowListItem(text = tunnel.name, onHold = {
                        if (state == Tunnel.State.UP && tunnel.name == tunnelName) {
                            scope.launch {
                                viewModel.showSnackBarMessage(context.resources.getString(R.string.turn_off_tunnel))
                            }
                            return@RowListItem
                        }
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        selectedTunnel = tunnel;
                    }, rowButton = {
                        if (tunnel.id == selectedTunnel?.id) {
                            Row() {
                                IconButton(onClick = {
                                    showAlertDialog = true
                                }) {
                                    Icon(Icons.Rounded.Edit, "Edit")
                                }
                                IconButton(onClick = { viewModel.onDelete(tunnel) }) {
                                    Icon(Icons.Rounded.Delete, "Delete")
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
            if (showAlertDialog && selectedTunnel != null) {
                AlertDialog(onDismissRequest = {
                    showAlertDialog = false
                }, confirmButton = {
                    Button(onClick = {
                        if (tunnels.any { it.name == selectedTunnel?.name }) {
                            Toast.makeText(context, context.resources.getString(R.string.tunnel_exists), Toast.LENGTH_LONG)
                                .show()
                            return@Button
                        }
                        viewModel.onEditTunnel(selectedTunnel!!)
                        showAlertDialog = false
                    }) {
                        Text("Save")
                    }
                },
                    title = { Text("Tunnel Edit") }, text = {
                        OutlinedTextField(
                            value = selectedTunnel!!.name,
                            onValueChange = {
                                selectedTunnel = selectedTunnel!!.copy(
                                    name = it
                                )
                            },
                            label = { Text("Tunnel Name") },
                            modifier = Modifier.padding(start = 15.dp, top = 5.dp),
                            maxLines = 1,
                        )
                    })
            }
        }
    }
}
