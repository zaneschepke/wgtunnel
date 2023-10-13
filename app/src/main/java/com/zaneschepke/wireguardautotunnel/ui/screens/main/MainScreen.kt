package com.zaneschepke.wireguardautotunnel.ui.screens.main

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Circle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.focus.onFocusChanged
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
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.wireguard.android.backend.Tunnel
import com.zaneschepke.wireguardautotunnel.Constants
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.WireGuardAutoTunnel
import com.zaneschepke.wireguardautotunnel.repository.model.TunnelConfig
import com.zaneschepke.wireguardautotunnel.service.tunnel.HandshakeStatus
import com.zaneschepke.wireguardautotunnel.ui.CaptureActivityPortrait
import com.zaneschepke.wireguardautotunnel.ui.Routes
import com.zaneschepke.wireguardautotunnel.ui.common.RowListItem
import com.zaneschepke.wireguardautotunnel.ui.theme.brickRed
import com.zaneschepke.wireguardautotunnel.ui.theme.mint
import com.zaneschepke.wireguardautotunnel.util.WgTunnelException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel = hiltViewModel(),
    padding: PaddingValues,
    showSnackbarMessage: (String) -> Unit,
    navController: NavController
) {

    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val isVisible = rememberSaveable { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }
    var showPrimaryChangeAlertDialog by remember { mutableStateOf(false) }
    val tunnels by viewModel.tunnels.collectAsStateWithLifecycle(mutableListOf())
    val handshakeStatus by viewModel.handshakeStatus.collectAsStateWithLifecycle(HandshakeStatus.NOT_STARTED)
    var selectedTunnel by remember { mutableStateOf<TunnelConfig?>(null) }
    val state by viewModel.state.collectAsStateWithLifecycle(Tunnel.State.DOWN)
    val tunnelName by viewModel.tunnelName.collectAsStateWithLifecycle("")
    val settings by viewModel.settings.collectAsStateWithLifecycle()

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

    val tunnelFileImportResultLauncher = rememberLauncherForActivityResult(object : ActivityResultContracts.GetContent() {
        override fun createIntent(context: Context, input: String): Intent {
            val intent = super.createIntent(context, input)

            /* AndroidTV now comes with stubs that do nothing but display a Toast less helpful than
             * what we can do, so detect this and throw an exception that we can catch later. */
            val activitiesToResolveIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()))
            } else {
                context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            }
            if (activitiesToResolveIntent.all {
                    val name = it.activityInfo.packageName
                    name.startsWith(Constants.GOOGLE_TV_EXPLORER_STUB) || name.startsWith(Constants.ANDROID_TV_EXPLORER_STUB)
                }) {
                throw WgTunnelException("No file explorer installed")
            }
            return intent
        }
    }) { data ->
        if (data == null) return@rememberLauncherForActivityResult
        scope.launch(Dispatchers.IO) {
            try {
                viewModel.onTunnelFileSelected(data)
            } catch (e : Exception) {
                showSnackbarMessage(e.message ?: "Unknown error occurred")
            }
        }
    }

    val scanLauncher = rememberLauncherForActivityResult(
        contract = ScanContract(),
        onResult = {
            try {
                viewModel.onTunnelQrResult(it.contents)
            } catch (e: Exception) {
                showSnackbarMessage(context.getString(R.string.qr_result_failed))
            }
        }
    )

    if(showPrimaryChangeAlertDialog) {
        AlertDialog(
            onDismissRequest = {
               showPrimaryChangeAlertDialog = false
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        viewModel.onDefaultTunnelChange(selectedTunnel)
                        showPrimaryChangeAlertDialog = false
                        selectedTunnel = null
                    }
                })
                { Text(text = stringResource(R.string.okay)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPrimaryChangeAlertDialog = false
                })
                { Text(text = stringResource(R.string.cancel)) }
            },
            title = { Text(text = stringResource(R.string.primary_tunnel_change)) },
            text = { Text(text = stringResource(R.string.primary_tunnnel_change_question)) }
        )
    }

    fun onTunnelToggle(checked : Boolean , tunnel : TunnelConfig) {
        try {
            if (checked) viewModel.onTunnelStart(tunnel) else viewModel.onTunnelStop()
        } catch (e : Exception) {
            showSnackbarMessage(e.message!!)
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
                val secondaryColor = MaterialTheme.colorScheme.secondary
                val hoverColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
                var fobColor by remember { mutableStateOf(secondaryColor) }
                FloatingActionButton(
                    modifier = Modifier.padding(bottom = 90.dp).onFocusChanged {
                        if(WireGuardAutoTunnel.isRunningOnAndroidTv(context)) {
                            fobColor = if (it.isFocused) hoverColor else secondaryColor }
                        }
                    ,
                    onClick = {
                        showBottomSheet = true
                    },
                    containerColor = fobColor,
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
                            try {
                                tunnelFileImportResultLauncher.launch(Constants.ALLOWED_FILE_TYPES)
                            } catch (e : Exception) {
                                showSnackbarMessage(e.message!!)
                            }
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
                if(!WireGuardAutoTunnel.isRunningOnAndroidTv(context)) {
                    Divider()
                    Row(modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            scope.launch {
                                showBottomSheet = false
                                val scanOptions = ScanOptions()
                                scanOptions.setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                                scanOptions.setOrientationLocked(true)
                                scanOptions.setPrompt(context.getString(R.string.scanning_qr))
                                scanOptions.setBeepEnabled(false)
                                scanOptions.captureActivity = CaptureActivityPortrait::class.java
                                scanLauncher.launch(scanOptions)
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
                Divider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showBottomSheet = false
                            navController.navigate("${Routes.Config.name}/${Constants.MANUAL_TUNNEL_CONFIG_ID}")
                        }
                        .padding(10.dp)
                ) {
                    Icon(
                        Icons.Filled.Create,
                        contentDescription = stringResource(id = R.string.create_import),
                        modifier = Modifier.padding(10.dp)
                    )
                    Text(
                        stringResource(id = R.string.create_import),
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
                items(tunnels, key = { tunnel -> tunnel.id }) { tunnel ->
                   val leadingIconColor = (if (tunnelName == tunnel.name) when (handshakeStatus) {
                        HandshakeStatus.HEALTHY -> mint
                        HandshakeStatus.UNHEALTHY -> brickRed
                        HandshakeStatus.NOT_STARTED -> Color.Gray
                        HandshakeStatus.NEVER_CONNECTED -> brickRed
                    } else {Color.Gray})
                    val focusRequester = remember { FocusRequester() }
                    RowListItem(icon = {
                        if (settings.isTunnelConfigDefault(tunnel))
                            Icon(
                                Icons.Rounded.Star, "status",
                                tint = leadingIconColor,
                                modifier =  Modifier.padding(end = 10.dp).size(20.dp)
                            )
                        else Icon(
                            Icons.Rounded.Circle, "status",
                            tint = leadingIconColor,
                            modifier =  Modifier.padding(end = 15.dp).size(15.dp)
                        )
                    },
                        text = tunnel.name,
                        onHold = {
                            if (state == Tunnel.State.UP && tunnel.name == tunnelName) {
                                showSnackbarMessage(context.resources.getString(R.string.turn_off_tunnel))
                                return@RowListItem
                            }
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            selectedTunnel = tunnel
                        },
                        onClick = {
                            if (!WireGuardAutoTunnel.isRunningOnAndroidTv(context)) {
                                navController.navigate("${Routes.Detail.name}/${tunnel.id}")
                            } else {
                                selectedTunnel = tunnel
                                focusRequester.requestFocus()
                            }
                        },
                        rowButton = {
                            if (tunnel.id == selectedTunnel?.id && !WireGuardAutoTunnel.isRunningOnAndroidTv(context)) {
                                Row {
                                    if(!settings.isTunnelConfigDefault(tunnel)) {
                                        IconButton(onClick = {
                                            if(settings.isAutoTunnelEnabled) {
                                                showSnackbarMessage(context.resources.getString(R.string.turn_off_auto))
                                            } else showPrimaryChangeAlertDialog = true
                                        }) {
                                            Icon(Icons.Rounded.Star, stringResource(id = R.string.set_primary))
                                        }
                                    }
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
                                        if(!settings.isTunnelConfigDefault(tunnel)) {
                                            IconButton(onClick = {
                                                if(settings.isAutoTunnelEnabled) {
                                                    showSnackbarMessage(context.resources.getString(R.string.turn_off_auto))
                                                } else showPrimaryChangeAlertDialog = true
                                            }) {
                                                Icon(Icons.Rounded.Star, stringResource(id = R.string.set_primary))
                                            }
                                        }
                                        IconButton(
                                            modifier = Modifier.focusRequester(focusRequester),
                                            onClick = {
                                                navController.navigate("${Routes.Detail.name}/${tunnel.id}")
                                            }) {
                                            Icon(Icons.Rounded.Info, "Info")
                                        }
                                        IconButton(onClick = {
                                            if (state == Tunnel.State.UP && tunnel.name == tunnelName)
                                                showSnackbarMessage(
                                                    context.resources.getString(
                                                        R.string.turn_off_tunnel
                                                    )
                                                )
                                            else {
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
                                                showSnackbarMessage(
                                                    context.resources.getString(
                                                        R.string.turn_off_tunnel
                                                    )
                                                )
                                            else {
                                                viewModel.onDelete(tunnel)
                                            }
                                        }) {
                                            Icon(
                                                Icons.Rounded.Delete,
                                                stringResource(id = R.string.delete)
                                            )
                                        }
                                        Switch(
                                            modifier = Modifier.focusRequester(focusRequester),
                                            checked = (state == Tunnel.State.UP && tunnel.name == tunnelName),
                                            onCheckedChange = { checked ->
                                                onTunnelToggle(checked, tunnel)
                                            }
                                        )
                                    }
                                } else {
                                    Switch(
                                        checked = (state == Tunnel.State.UP && tunnel.name == tunnelName),
                                        onCheckedChange = { checked ->
                                            onTunnelToggle(checked, tunnel)
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
