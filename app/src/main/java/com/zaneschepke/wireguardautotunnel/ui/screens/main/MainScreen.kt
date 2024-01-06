package com.zaneschepke.wireguardautotunnel.ui.screens.main

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.overscroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bolt
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
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
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
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.WireGuardAutoTunnel
import com.zaneschepke.wireguardautotunnel.data.model.TunnelConfig
import com.zaneschepke.wireguardautotunnel.service.tunnel.HandshakeStatus
import com.zaneschepke.wireguardautotunnel.ui.CaptureActivityPortrait
import com.zaneschepke.wireguardautotunnel.ui.Screen
import com.zaneschepke.wireguardautotunnel.ui.common.RowListItem
import com.zaneschepke.wireguardautotunnel.ui.common.screen.LoadingScreen
import com.zaneschepke.wireguardautotunnel.ui.theme.corn
import com.zaneschepke.wireguardautotunnel.ui.theme.mint
import com.zaneschepke.wireguardautotunnel.util.Constants
import com.zaneschepke.wireguardautotunnel.util.Event
import com.zaneschepke.wireguardautotunnel.util.Result
import com.zaneschepke.wireguardautotunnel.util.handshakeStatus
import com.zaneschepke.wireguardautotunnel.util.mapPeerStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel = hiltViewModel(),
    padding: PaddingValues,
    focusRequester: FocusRequester,
    showSnackbarMessage: (String) -> Unit,
    navController: NavController
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val isVisible = rememberSaveable { mutableStateOf(true) }
    val scope = rememberCoroutineScope { Dispatchers.IO }

    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }

    var showPrimaryChangeAlertDialog by remember { mutableStateOf(false) }
    var showDeleteTunnelAlertDialog by remember { mutableStateOf(false) }
    var selectedTunnel by remember { mutableStateOf<TunnelConfig?>(null) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.loading) {
        if (!uiState.loading && WireGuardAutoTunnel.isRunningOnAndroidTv()) {
            delay(Constants.FOCUS_REQUEST_DELAY)
            focusRequester.requestFocus()
        }
    }

    if (uiState.loading) {
        LoadingScreen()
        return
    }

    val tunnelFileImportResultLauncher =
        rememberLauncherForActivityResult(
            object : ActivityResultContracts.GetContent() {
                override fun createIntent(context: Context, input: String): Intent {
                    val intent = super.createIntent(context, input)

                    /* AndroidTV now comes with stubs that do nothing but display a Toast less helpful than
                     * what we can do, so detect this and throw an exception that we can catch later. */
                    val activitiesToResolveIntent =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            context.packageManager.queryIntentActivities(
                                intent,
                                PackageManager.ResolveInfoFlags.of(
                                    PackageManager.MATCH_DEFAULT_ONLY.toLong(),
                                ),
                            )
                        } else {
                            context.packageManager.queryIntentActivities(
                                intent,
                                PackageManager.MATCH_DEFAULT_ONLY,
                            )
                        }
                    if (
                        activitiesToResolveIntent.all {
                            val name = it.activityInfo.packageName
                            name.startsWith(Constants.GOOGLE_TV_EXPLORER_STUB) ||
                                name.startsWith(Constants.ANDROID_TV_EXPLORER_STUB)
                        }
                    ) {
                        showSnackbarMessage(Event.Error.FileExplorerRequired.message)
                    }
                    return intent
                }
            },
        ) { data ->
            if (data == null) return@rememberLauncherForActivityResult
            scope.launch {
                viewModel.onTunnelFileSelected(data).let {
                    when (it) {
                        is Result.Error -> showSnackbarMessage(it.error.message)
                        is Result.Success -> {}
                    }
                }
            }
        }
    val scanLauncher =
        rememberLauncherForActivityResult(
            contract = ScanContract(),
            onResult = {
                if (it.contents != null) {
                    scope.launch {
                        viewModel.onTunnelQrResult(it.contents).let { result ->
                            when (result) {
                                is Result.Success -> {}
                                is Result.Error -> showSnackbarMessage(result.error.message)
                            }
                        }
                    }
                }
            },
        )

    AnimatedVisibility(showPrimaryChangeAlertDialog) {
        AlertDialog(
            onDismissRequest = { showPrimaryChangeAlertDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.onDefaultTunnelChange(selectedTunnel)
                        showPrimaryChangeAlertDialog = false
                        selectedTunnel = null
                    },
                ) {
                    Text(text = stringResource(R.string.okay))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPrimaryChangeAlertDialog = false }) {
                    Text(text = stringResource(R.string.cancel))
                }
            },
            title = { Text(text = stringResource(R.string.primary_tunnel_change)) },
            text = { Text(text = stringResource(R.string.primary_tunnel_change_question)) },
        )
    }

    AnimatedVisibility(showDeleteTunnelAlertDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteTunnelAlertDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedTunnel?.let { viewModel.onDelete(it) }
                        showDeleteTunnelAlertDialog = false
                        selectedTunnel = null
                    },
                ) {
                    Text(text = stringResource(R.string.yes))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteTunnelAlertDialog = false }) {
                    Text(text = stringResource(R.string.cancel))
                }
            },
            title = { Text(text = stringResource(R.string.delete_tunnel)) },
            text = { Text(text = stringResource(R.string.delete_tunnel_message)) },
        )
    }

    fun onTunnelToggle(checked: Boolean, tunnel: TunnelConfig) {
        if (checked) viewModel.onTunnelStart(tunnel) else viewModel.onTunnelStop()
    }

    Scaffold(
        modifier =
            Modifier.pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        if (!WireGuardAutoTunnel.isRunningOnAndroidTv()) selectedTunnel = null
                    },
                )
            },
        floatingActionButtonPosition = FabPosition.End,
        topBar = {
            if (uiState.settings.isAutoTunnelEnabled)
                TopAppBar(
                    title = {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier =
                                Modifier.requiredWidth(LocalConfiguration.current.screenWidthDp.dp)
                                    .padding(end = 5.dp),
                        ) {
                            Row {
                                Icon(
                                    Icons.Rounded.Bolt,
                                    stringResource(id = R.string.auto),
                                    modifier = Modifier.size(25.dp),
                                    tint =
                                        if (uiState.settings.isAutoTunnelPaused) Color.Gray
                                        else mint,
                                )
                                Text(
                                    "Auto-tunneling: ${if (uiState.settings.isAutoTunnelPaused) "paused" else "active"}",
                                    style = typography.bodyLarge,
                                    modifier = Modifier.padding(start = 10.dp),
                                )
                            }
                            if (uiState.settings.isAutoTunnelPaused)
                                TextButton(
                                    onClick = { viewModel.resumeAutoTunneling() },
                                    modifier = Modifier.padding(end = 10.dp),
                                ) {
                                    Text("Resume")
                                }
                            else
                                TextButton(
                                    onClick = { viewModel.pauseAutoTunneling() },
                                    modifier = Modifier.padding(end = 10.dp),
                                ) {
                                    Text("Pause")
                                }
                        }
                    },
                )
        },
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
                    modifier =
                        (if (
                                WireGuardAutoTunnel.isRunningOnAndroidTv() &&
                                    uiState.tunnels.isEmpty()
                            )
                                Modifier.focusRequester(focusRequester)
                            else Modifier)
                            .padding(bottom = 90.dp)
                            .onFocusChanged {
                                if (WireGuardAutoTunnel.isRunningOnAndroidTv()) {
                                    fobColor = if (it.isFocused) hoverColor else secondaryColor
                                }
                            },
                    onClick = { showBottomSheet = true },
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
        },
    ) { innerPadding ->
        AnimatedVisibility(uiState.tunnels.isEmpty(), exit = fadeOut(), enter = fadeIn()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize().padding(padding),
            ) {
                Text(text = stringResource(R.string.no_tunnels), fontStyle = FontStyle.Italic)
            }
        }
        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showBottomSheet = false },
                sheetState = sheetState,
            ) {
                // Sheet content
                Row(
                    modifier =
                        Modifier.fillMaxWidth()
                            .clickable {
                                showBottomSheet = false
                                tunnelFileImportResultLauncher.launch(Constants.ALLOWED_FILE_TYPES)
                            }
                            .padding(10.dp),
                ) {
                    Icon(
                        Icons.Filled.FileOpen,
                        contentDescription = stringResource(id = R.string.open_file),
                        modifier = Modifier.padding(10.dp),
                    )
                    Text(
                        stringResource(id = R.string.add_tunnels_text),
                        modifier = Modifier.padding(10.dp),
                    )
                }
                if (!WireGuardAutoTunnel.isRunningOnAndroidTv()) {
                    Divider()
                    Row(
                        modifier =
                            Modifier.fillMaxWidth()
                                .clickable {
                                    scope.launch {
                                        showBottomSheet = false
                                        val scanOptions = ScanOptions()
                                        scanOptions.setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                                        scanOptions.setOrientationLocked(true)
                                        scanOptions.setPrompt(
                                            context.getString(R.string.scanning_qr)
                                        )
                                        scanOptions.setBeepEnabled(false)
                                        scanOptions.captureActivity =
                                            CaptureActivityPortrait::class.java
                                        scanLauncher.launch(scanOptions)
                                    }
                                }
                                .padding(10.dp),
                    ) {
                        Icon(
                            Icons.Filled.QrCode,
                            contentDescription = stringResource(id = R.string.qr_scan),
                            modifier = Modifier.padding(10.dp),
                        )
                        Text(
                            stringResource(id = R.string.add_from_qr),
                            modifier = Modifier.padding(10.dp),
                        )
                    }
                }
                Divider()
                Row(
                    modifier =
                        Modifier.fillMaxWidth()
                            .clickable {
                                showBottomSheet = false
                                navController.navigate(
                                    "${Screen.Config.route}/${Constants.MANUAL_TUNNEL_CONFIG_ID}",
                                )
                            }
                            .padding(10.dp),
                ) {
                    Icon(
                        Icons.Filled.Create,
                        contentDescription = stringResource(id = R.string.create_import),
                        modifier = Modifier.padding(10.dp),
                    )
                    Text(
                        stringResource(id = R.string.create_import),
                        modifier = Modifier.padding(10.dp),
                    )
                }
            }
        }

        LazyColumn(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Top,
            modifier =
                Modifier.fillMaxWidth()
                    .fillMaxHeight(.90f)
                    .overscroll(ScrollableDefaults.overscrollEffect())
                    .padding(innerPadding),
            state = rememberLazyListState(0, uiState.tunnels.count()),
            userScrollEnabled = true,
            reverseLayout = true,
            flingBehavior = ScrollableDefaults.flingBehavior(),
        ) {
            items(
                uiState.tunnels,
                key = { tunnel -> tunnel.id },
            ) { tunnel ->
                val leadingIconColor =
                    (if (
                        uiState.vpnState.name == tunnel.name &&
                            uiState.vpnState.status == Tunnel.State.UP
                    ) {
                        uiState.vpnState.statistics
                            ?.mapPeerStats()
                            ?.map { it.value?.handshakeStatus() }
                            .let { statuses ->
                                when {
                                    statuses?.all { it == HandshakeStatus.HEALTHY } == true -> mint
                                    statuses?.any { it == HandshakeStatus.STALE } == true -> corn
                                    statuses?.all { it == HandshakeStatus.NOT_STARTED } == true ->
                                        Color.Gray
                                    else -> {
                                        Color.Gray
                                    }
                                }
                            }
                    } else {
                        Color.Gray
                    })
                val expanded = remember { mutableStateOf(false) }
                RowListItem(
                    icon = {
                        if (uiState.settings.isTunnelConfigDefault(tunnel)) {
                            Icon(
                                Icons.Rounded.Star,
                                stringResource(R.string.status),
                                tint = leadingIconColor,
                                modifier = Modifier.padding(end = 10.dp).size(20.dp),
                            )
                        } else {
                            Icon(
                                Icons.Rounded.Circle,
                                stringResource(R.string.status),
                                tint = leadingIconColor,
                                modifier = Modifier.padding(end = 15.dp).size(15.dp),
                            )
                        }
                    },
                    text = tunnel.name,
                    onHold = {
                        if (
                            (uiState.vpnState.status == Tunnel.State.UP) &&
                                (tunnel.name == uiState.vpnState.name)
                        ) {
                            showSnackbarMessage(Event.Message.TunnelOffAction.message)
                            return@RowListItem
                        }
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        selectedTunnel = tunnel
                    },
                    onClick = {
                        if (!WireGuardAutoTunnel.isRunningOnAndroidTv()) {
                            if (
                                uiState.vpnState.status == Tunnel.State.UP &&
                                    (uiState.vpnState.name == tunnel.name)
                            ) {
                                expanded.value = !expanded.value
                            }
                        } else {
                            selectedTunnel = tunnel
                            focusRequester.requestFocus()
                        }
                    },
                    statistics = uiState.vpnState.statistics,
                    expanded = expanded.value,
                    rowButton = {
                        if (
                            tunnel.id == selectedTunnel?.id &&
                                !WireGuardAutoTunnel.isRunningOnAndroidTv()
                        ) {
                            Row {
                                if (!uiState.settings.isTunnelConfigDefault(tunnel)) {
                                    IconButton(
                                        onClick = {
                                            if (
                                                uiState.settings.isAutoTunnelEnabled &&
                                                    !uiState.settings.isAutoTunnelPaused
                                            ) {
                                                showSnackbarMessage(
                                                    Event.Message.AutoTunnelOffAction.message,
                                                )
                                            } else {
                                                showPrimaryChangeAlertDialog = true
                                            }
                                        },
                                    ) {
                                        Icon(
                                            Icons.Rounded.Star,
                                            stringResource(id = R.string.set_primary),
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = {
                                        if (
                                            uiState.settings.isAutoTunnelEnabled &&
                                                uiState.settings.isTunnelConfigDefault(
                                                    tunnel,
                                                ) &&
                                                !uiState.settings.isAutoTunnelPaused
                                        ) {
                                            showSnackbarMessage(
                                                Event.Message.AutoTunnelOffAction.message,
                                            )
                                        } else
                                            navController.navigate(
                                                "${Screen.Config.route}/${selectedTunnel?.id}",
                                            )
                                    },
                                ) {
                                    Icon(Icons.Rounded.Edit, stringResource(id = R.string.edit))
                                }
                                IconButton(
                                    modifier = Modifier.focusable(),
                                    onClick = { showDeleteTunnelAlertDialog = true },
                                ) {
                                    Icon(Icons.Rounded.Delete, stringResource(id = R.string.delete))
                                }
                            }
                        } else {
                            val checked by remember {
                                derivedStateOf {
                                    (uiState.vpnState.status == Tunnel.State.UP &&
                                        tunnel.name == uiState.vpnState.name)
                                }
                            }
                            if (!checked) expanded.value = false

                            @Composable
                            fun TunnelSwitch() =
                                Switch(
                                    modifier = Modifier.focusRequester(focusRequester),
                                    checked = checked,
                                    onCheckedChange = { checked ->
                                        if (!checked) expanded.value = false
                                        onTunnelToggle(checked, tunnel)
                                    },
                                )
                            if (WireGuardAutoTunnel.isRunningOnAndroidTv()) {
                                Row {
                                    if (!uiState.settings.isTunnelConfigDefault(tunnel)) {
                                        IconButton(
                                            onClick = {
                                                if (uiState.settings.isAutoTunnelEnabled) {
                                                    showSnackbarMessage(
                                                        Event.Message.AutoTunnelOffAction.message,
                                                    )
                                                } else {
                                                    selectedTunnel = tunnel
                                                    showPrimaryChangeAlertDialog = true
                                                }
                                            },
                                        ) {
                                            Icon(
                                                Icons.Rounded.Star,
                                                stringResource(id = R.string.set_primary),
                                            )
                                        }
                                    }
                                    IconButton(
                                        modifier = Modifier.focusRequester(focusRequester),
                                        onClick = {
                                            if (
                                                uiState.vpnState.status == Tunnel.State.UP &&
                                                    (uiState.vpnState.name == tunnel.name)
                                            ) {
                                                expanded.value = !expanded.value
                                            } else {
                                                showSnackbarMessage(
                                                    Event.Message.TunnelOnAction.message
                                                )
                                            }
                                        },
                                    ) {
                                        Icon(Icons.Rounded.Info, stringResource(R.string.info))
                                    }
                                    IconButton(
                                        onClick = {
                                            if (
                                                uiState.vpnState.status == Tunnel.State.UP &&
                                                    tunnel.name == uiState.vpnState.name
                                            ) {
                                                showSnackbarMessage(
                                                    Event.Message.TunnelOffAction.message
                                                )
                                            } else {
                                                navController.navigate(
                                                    "${Screen.Config.route}/${tunnel.id}",
                                                )
                                            }
                                        },
                                    ) {
                                        Icon(Icons.Rounded.Edit, stringResource(id = R.string.edit))
                                    }
                                    IconButton(
                                        onClick = {
                                            if (
                                                uiState.vpnState.status == Tunnel.State.UP &&
                                                    tunnel.name == uiState.vpnState.name
                                            ) {
                                                showSnackbarMessage(
                                                    Event.Message.TunnelOffAction.message
                                                )
                                            } else {
                                                showDeleteTunnelAlertDialog = true
                                            }
                                        },
                                    ) {
                                        Icon(
                                            Icons.Rounded.Delete,
                                            stringResource(id = R.string.delete),
                                        )
                                    }
                                    TunnelSwitch()
                                }
                            } else {
                                TunnelSwitch()
                            }
                        }
                    },
                )
            }
        }
    }
}
