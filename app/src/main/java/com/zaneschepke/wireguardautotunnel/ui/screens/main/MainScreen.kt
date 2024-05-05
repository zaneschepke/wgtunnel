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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.overscroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Circle
import androidx.compose.material.icons.rounded.CopyAll
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.WireGuardAutoTunnel
import com.zaneschepke.wireguardautotunnel.data.domain.TunnelConfig
import com.zaneschepke.wireguardautotunnel.service.tunnel.HandshakeStatus
import com.zaneschepke.wireguardautotunnel.service.tunnel.TunnelState
import com.zaneschepke.wireguardautotunnel.ui.AppViewModel
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
import com.zaneschepke.wireguardautotunnel.util.truncateWithEllipsis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Timer

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel = hiltViewModel(),
    appViewModel: AppViewModel,
    focusRequester: FocusRequester,
    navController: NavController
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val isVisible = rememberSaveable { mutableStateOf(true) }
    val scope = rememberCoroutineScope { Dispatchers.IO }

    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }

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


    var showDeleteTunnelAlertDialog by remember { mutableStateOf(false) }
    var selectedTunnel by remember { mutableStateOf<TunnelConfig?>(null) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        if (WireGuardAutoTunnel.isRunningOnAndroidTv()) {
            delay(Constants.FOCUS_REQUEST_DELAY)
            focusRequester.requestFocus()
        }
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
                        appViewModel.showSnackbarMessage(Event.Error.FileExplorerRequired.message)
                    }
                    return intent
                }
            },
        ) { data ->
            if (data == null) return@rememberLauncherForActivityResult
            scope.launch {
                viewModel.onTunnelFileSelected(data).let {
                    when (it) {
                        is Result.Error -> appViewModel.showSnackbarMessage(it.error.message)
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
                                is Result.Error -> appViewModel.showSnackbarMessage(result.error.message)
                            }
                        }
                    }
                }
            },
        )

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
        if (appViewModel.isRequiredPermissionGranted()) {
            if (checked) viewModel.onTunnelStart(tunnel) else viewModel.onTunnelStop()
        }
    }

    if (uiState.loading) {
        return LoadingScreen()
    }

    Scaffold(
        modifier =
        Modifier.pointerInput(Unit) {
            if(uiState.tunnels.isNotEmpty()) {
                detectTapGestures(
                    onTap = {
                        selectedTunnel = null
                    },
                )
            }

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
                    modifier =
                    (if (
                        WireGuardAutoTunnel.isRunningOnAndroidTv() &&
                        uiState.tunnels.isEmpty()
                    )
                        Modifier.focusRequester(focusRequester)
                    else Modifier)
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
    ) {
        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showBottomSheet = false },
                sheetState = sheetState,
            ) {
                // Sheet content
                Row(
                    modifier =
                    Modifier
                        .fillMaxWidth()
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
                    HorizontalDivider()
                    Row(
                        modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                scope.launch {
                                    showBottomSheet = false
                                    val scanOptions = ScanOptions()
                                    scanOptions.setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                                    scanOptions.setOrientationLocked(true)
                                    scanOptions.setPrompt(
                                        context.getString(R.string.scanning_qr),
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
                HorizontalDivider()
                Row(
                    modifier =
                    Modifier
                        .fillMaxWidth()
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
            Modifier
                .fillMaxSize()
                .overscroll(ScrollableDefaults.overscrollEffect())
                .nestedScroll(nestedScrollConnection),
            state = rememberLazyListState(0, uiState.tunnels.count()),
            userScrollEnabled = true,
            reverseLayout = false,
            flingBehavior = ScrollableDefaults.flingBehavior(),
        ) {
            item {
                val gettingStarted = buildAnnotatedString {
                    append(stringResource(id = R.string.see_the))
                    append(" ")
                    pushStringAnnotation(tag = "gettingStarted", annotation = stringResource(id = R.string.getting_started_url))
                    withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                        append(stringResource(id = R.string.getting_started_guide))
                    }
                    pop()
                    append(" ")
                    append(stringResource(R.string.unsure_how))
                    append(".")
                }
                AnimatedVisibility(
                    uiState.tunnels.isEmpty(), exit = fadeOut(), enter = fadeIn()) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(top = 100.dp)
                    ) {
                        Text(text = stringResource(R.string.no_tunnels), fontStyle = FontStyle.Italic)
                        ClickableText(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 24.dp),
                            text = gettingStarted,
                            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center),
                        ) {
                            gettingStarted.getStringAnnotations(tag = "gettingStarted", it, it).firstOrNull()?.let { annotation ->
                                appViewModel.openWebPage(annotation.item, context)
                            }
                        }
                    }
                }
            }
            item {
                if (uiState.settings.isAutoTunnelEnabled) {
                    val autoTunnelingLabel = buildAnnotatedString {
                        append(stringResource(id = R.string.auto_tunneling))
                        append(": ")
                        if (uiState.settings.isAutoTunnelPaused) append(
                            stringResource(id = R.string.paused),
                        ) else append(
                            stringResource(id = R.string.active),
                        )
                    }
                    RowListItem(
                        icon = {
                            val icon = Icons.Rounded.Bolt
                            Icon(
                                icon,
                                icon.name,
                                modifier = Modifier
                                    .padding(end = 8.5.dp)
                                    .size(25.dp),
                                tint =
                                if (uiState.settings.isAutoTunnelPaused) Color.Gray
                                else mint,
                            )
                        },
                        text = autoTunnelingLabel.text,
                        rowButton = {
                            if (uiState.settings.isAutoTunnelPaused) {
                                TextButton(
                                    onClick = { viewModel.resumeAutoTunneling() },
                                ) {
                                    Text(stringResource(id = R.string.resume))
                                }
                            } else {
                                TextButton(
                                    onClick = { viewModel.pauseAutoTunneling() },
                                ) {
                                    Text(stringResource(id = R.string.pause))
                                }
                            }
                        },
                        onClick = {},
                        onHold = {},
                        expanded = false,
                        statistics = null,
                    )
                }
            }
            items(
                uiState.tunnels,
                key = { tunnel -> tunnel.id },
            ) { tunnel ->
                val leadingIconColor =
                    (if (
                        uiState.vpnState.tunnelConfig?.name == tunnel.name &&
                        uiState.vpnState.status == TunnelState.UP
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
                        val circleIcon = Icons.Rounded.Circle
                        val icon = if (tunnel.isPrimaryTunnel) {
                            Icons.Rounded.Star
                        } else if (tunnel.isMobileDataTunnel) {
                            Icons.Rounded.Smartphone
                        } else {
                            circleIcon
                        }
                        Icon(
                            icon,
                            icon.name,
                            tint = leadingIconColor,
                            modifier = Modifier
                                .padding(
                                    end = if (icon == circleIcon) 12.5.dp else 10.dp,
                                    start = if (icon == circleIcon) 2.5.dp else 0.dp,
                                )
                                .size(if (icon == circleIcon) 15.dp else 20.dp),
                        )
                    },
                    text = tunnel.name.truncateWithEllipsis(Constants.ALLOWED_DISPLAY_NAME_LENGTH),
                    onHold = {
                        if (
                            (uiState.vpnState.status == TunnelState.UP) &&
                            (tunnel.name == uiState.vpnState.tunnelConfig?.name)
                        ) {
                            appViewModel.showSnackbarMessage(Event.Message.TunnelOffAction.message)
                            return@RowListItem
                        }
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        selectedTunnel = tunnel
                    },
                    onClick = {
                        if (!WireGuardAutoTunnel.isRunningOnAndroidTv()) {
                            if (
                                uiState.vpnState.status == TunnelState.UP &&
                                (uiState.vpnState.tunnelConfig?.name == tunnel.name)
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
                                IconButton(
                                    onClick = {
                                        if (
                                            uiState.settings.isAutoTunnelEnabled &&
                                            !uiState.settings.isAutoTunnelPaused
                                        ) {
                                            appViewModel.showSnackbarMessage(
                                                Event.Message.AutoTunnelOffAction.message,
                                            )
                                        } else {
                                            navController.navigate(
                                                "${Screen.Option.route}/${selectedTunnel?.id}",
                                            )
                                        }
                                    },
                                ) {
                                    val icon = Icons.Rounded.Settings
                                    Icon(
                                        icon,
                                        icon.name,
                                    )
                                }
                                IconButton(
                                    modifier = Modifier.focusable(),
                                    onClick = { viewModel.onCopyTunnel(selectedTunnel) },
                                ) {
                                    val icon = Icons.Rounded.CopyAll
                                    Icon(icon, icon.name)
                                }
                                IconButton(
                                    modifier = Modifier.focusable(),
                                    onClick = { showDeleteTunnelAlertDialog = true },
                                ) {
                                    val icon = Icons.Rounded.Delete
                                    Icon(icon, icon.name)
                                }
                            }
                        } else {
                            val checked by remember {
                                derivedStateOf {
                                    (uiState.vpnState.status == TunnelState.UP &&
                                        tunnel.name == uiState.vpnState.tunnelConfig?.name)
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
                                    IconButton(
                                        onClick = {
                                            if (uiState.settings.isAutoTunnelEnabled) {
                                                appViewModel.showSnackbarMessage(
                                                    Event.Message.AutoTunnelOffAction.message,
                                                )
                                            } else {
                                                selectedTunnel = tunnel
                                                navController.navigate(
                                                    "${Screen.Option.route}/${selectedTunnel?.id}",
                                                )
                                            }
                                        },
                                    ) {
                                        val icon = Icons.Rounded.Settings
                                        Icon(
                                            icon,
                                            icon.name,
                                        )
                                    }
                                    IconButton(
                                        modifier = Modifier.focusRequester(focusRequester),
                                        onClick = {
                                            if (
                                                uiState.vpnState.status == TunnelState.UP &&
                                                (uiState.vpnState.tunnelConfig?.name == tunnel.name)
                                            ) {
                                                expanded.value = !expanded.value
                                            } else {
                                                appViewModel.showSnackbarMessage(
                                                    Event.Message.TunnelOnAction.message,
                                                )
                                            }
                                        },
                                    ) {
                                        val icon = Icons.Rounded.Info
                                        Icon(icon, icon.name)
                                    }
                                    IconButton(
                                        onClick = { viewModel.onCopyTunnel(tunnel) },
                                    ) {
                                        val icon = Icons.Rounded.CopyAll
                                        Icon(icon, icon.name)
                                    }
                                    IconButton(
                                        onClick = {
                                            if (
                                                uiState.vpnState.status == TunnelState.UP &&
                                                tunnel.name == uiState.vpnState.tunnelConfig?.name
                                            ) {
                                                appViewModel.showSnackbarMessage(
                                                    Event.Message.TunnelOffAction.message,
                                                )
                                            } else {
                                                selectedTunnel = tunnel
                                                showDeleteTunnelAlertDialog = true
                                            }
                                        },
                                    ) {
                                        val icon = Icons.Rounded.Delete
                                        Icon(
                                            icon,
                                            icon.name,
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
