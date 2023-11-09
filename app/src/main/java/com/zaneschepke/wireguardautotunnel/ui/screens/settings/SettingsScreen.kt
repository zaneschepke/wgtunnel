package com.zaneschepke.wireguardautotunnel.ui.screens.settings

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.rounded.LocationOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.WireGuardAutoTunnel
import com.zaneschepke.wireguardautotunnel.ui.common.ClickableIconButton
import com.zaneschepke.wireguardautotunnel.ui.common.config.ConfigurationToggle
import com.zaneschepke.wireguardautotunnel.ui.common.prompt.AuthorizationPrompt
import com.zaneschepke.wireguardautotunnel.ui.common.text.SectionTitle
import com.zaneschepke.wireguardautotunnel.util.StorageUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

@OptIn(
    ExperimentalPermissionsApi::class,
    ExperimentalLayoutApi::class, ExperimentalComposeUiApi::class
)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    padding: PaddingValues,
    showSnackbarMessage: (String) -> Unit,
    focusRequester: FocusRequester,
) {

    val scope = rememberCoroutineScope { Dispatchers.IO }
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val interactionSource = remember { MutableInteractionSource() }

    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val trustedSSIDs by viewModel.trustedSSIDs.collectAsStateWithLifecycle()
    val tunnels by viewModel.tunnels.collectAsStateWithLifecycle(mutableListOf())
    val fineLocationState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    var currentText by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()
    var isLocationDisclaimerNeeded by remember { mutableStateOf(true) }
    var isBackgroundLocationGranted by remember { mutableStateOf(true) }
    var showAuthPrompt by remember { mutableStateOf(false) }
    var didExportFiles by remember { mutableStateOf(false) }

    val screenPadding = 5.dp
    val fillMaxWidth = .85f

    fun exportAllConfigs() {
        try {
            val files = tunnels.map { File(context.cacheDir, "${it.name}.conf") }
            files.forEachIndexed { index, file ->
                file.outputStream().use {
                    it.write(tunnels[index].wgQuick.toByteArray())
                }
            }
            StorageUtil.saveFilesToZip(context, files)
            didExportFiles = true
            showSnackbarMessage(context.getString(R.string.exported_configs_message))
        } catch (e : Exception) {
            showSnackbarMessage(e.message!!)
        }
    }


    fun saveTrustedSSID() {
        if (currentText.isNotEmpty()) {
            scope.launch {
                try {
                    viewModel.onSaveTrustedSSID(currentText)
                    currentText = ""
                } catch (e : Exception) {
                    showSnackbarMessage(e.message ?: context.getString(R.string.unknown_error))
                }
            }
        }
    }

    fun isAllAutoTunnelPermissionsEnabled() : Boolean {
        return(isBackgroundLocationGranted && fineLocationState.status.isGranted && !viewModel.isLocationServicesNeeded())
    }


    fun openSettings() {
        scope.launch {
            val intentSettings =
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intentSettings.data =
                Uri.fromParts("package", context.packageName, null)
            context.startActivity(intentSettings)
        }
    }

    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val backgroundLocationState =
            rememberPermissionState(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        if(!backgroundLocationState.status.isGranted) {
            isBackgroundLocationGranted = false
        } else {
            isLocationDisclaimerNeeded = false
            isBackgroundLocationGranted = true
        }
    }

    if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
        if(!fineLocationState.status.isGranted) {
            isBackgroundLocationGranted = false
        } else {
            isLocationDisclaimerNeeded = false
            isBackgroundLocationGranted = true
        }
    }

    if(isLocationDisclaimerNeeded) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(padding)
        ) {
            Icon(
                Icons.Rounded.LocationOff,
                contentDescription = stringResource(id = R.string.map),
                modifier = Modifier
                    .padding(30.dp)
                    .size(128.dp)
            )
            Text(
                stringResource(R.string.prominent_background_location_title),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(30.dp),
                fontSize = 20.sp
            )
            Text(
                stringResource(R.string.prominent_background_location_message),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(30.dp),
                fontSize = 15.sp
            )
            Row(
                modifier = if (WireGuardAutoTunnel.isRunningOnAndroidTv(context)) Modifier
                    .fillMaxWidth()
                    .padding(10.dp) else Modifier
                    .fillMaxWidth()
                    .padding(30.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TextButton(onClick = {
                    isLocationDisclaimerNeeded = false
                }) {
                    Text(stringResource(id = R.string.no_thanks))
                }
                TextButton(modifier = Modifier.focusRequester(focusRequester), onClick = {
                    openSettings()
                }) {
                    Text(stringResource(id = R.string.turn_on))
                }
            }
        }
        return
    }



    if(showAuthPrompt) {
        AuthorizationPrompt(onSuccess = {
            showAuthPrompt = false
            exportAllConfigs() },
            onError = { error ->
                showSnackbarMessage(error)
                showAuthPrompt = false
                },
            onFailure = {
                showAuthPrompt = false
                showSnackbarMessage(context.getString(R.string.authentication_failed))
            })
    }

    if (tunnels.isEmpty()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Text(
                stringResource(R.string.one_tunnel_required),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(15.dp),
                fontStyle = FontStyle.Italic
            )
        }
        return
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .clickable(indication = null, interactionSource = interactionSource) {
                focusManager.clearFocus()
            }
    ) {
        Surface(
            tonalElevation = 2.dp,
            shadowElevation = 2.dp,
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = (if (WireGuardAutoTunnel.isRunningOnAndroidTv(context))
                Modifier
                    .height(IntrinsicSize.Min)
                    .fillMaxWidth(fillMaxWidth)
            else Modifier.fillMaxWidth(fillMaxWidth)).padding(top = 60.dp, bottom = 25.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Top,
                modifier = Modifier.padding(15.dp)
            ) {
                SectionTitle(title = stringResource(id = R.string.auto_tunneling), padding = screenPadding)
                Text(
                    stringResource(R.string.trusted_ssid),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(screenPadding, bottom = 5.dp, top = 5.dp)
                )
                FlowRow(
                    modifier = Modifier.padding(screenPadding),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    trustedSSIDs.forEach { ssid ->
                        ClickableIconButton(
                            onIconClick = {
                                scope.launch {
                                    viewModel.onDeleteTrustedSSID(ssid)
                                }
                            },
                            text = ssid,
                            icon = Icons.Filled.Close,
                            enabled = !(settings.isAutoTunnelEnabled || settings.isAlwaysOnVpnEnabled)
                        )
                    }
                    if(trustedSSIDs.isEmpty()) {
                        Text(stringResource(R.string.none), fontStyle = FontStyle.Italic, color = Color.Gray)
                    }
                }
                OutlinedTextField(
                    enabled = !(settings.isAutoTunnelEnabled || settings.isAlwaysOnVpnEnabled),
                    value = currentText,
                    onValueChange = { currentText = it },
                    label = { Text(stringResource(R.string.add_trusted_ssid)) },
                    modifier = Modifier.padding(start = screenPadding, top = 5.dp).focusRequester(focusRequester).onFocusChanged {
                        if(WireGuardAutoTunnel.isRunningOnAndroidTv(context)) {
                            keyboardController?.hide()
                        }
                    },
                    maxLines = 1,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            saveTrustedSSID()
                        }
                    ),
                    trailingIcon = {
                        IconButton(onClick = { saveTrustedSSID() }) {
                            Icon(
                                imageVector = Icons.Outlined.Add,
                                contentDescription = if (currentText == "") stringResource(id = R.string.trusted_ssid_empty_description) else stringResource(
                                    id = R.string.trusted_ssid_value_description
                                ),
                                tint = if (currentText == "") Color.Transparent else MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                )
                ConfigurationToggle(stringResource(R.string.tunnel_mobile_data),
                    enabled = !(settings.isAutoTunnelEnabled || settings.isAlwaysOnVpnEnabled),
                    checked = settings.isTunnelOnMobileDataEnabled,
                    padding = screenPadding,
                    onCheckChanged = {
                        scope.launch {
                            viewModel.onToggleTunnelOnMobileData()
                        }
                    }
                )
                ConfigurationToggle(stringResource(id = R.string.tunnel_on_ethernet),
                    enabled = !(settings.isAutoTunnelEnabled || settings.isAlwaysOnVpnEnabled),
                    checked = settings.isTunnelOnEthernetEnabled,
                    padding = screenPadding,
                    onCheckChanged = {
                        scope.launch {
                            viewModel.onToggleTunnelOnEthernet()
                        }
                    }
                )
                ConfigurationToggle(
                    stringResource(R.string.battery_saver),
                    enabled = !(settings.isAutoTunnelEnabled || settings.isAlwaysOnVpnEnabled),
                    checked = settings.isBatterySaverEnabled,
                    padding = screenPadding,
                    onCheckChanged = {
                        scope.launch {
                            viewModel.onToggleBatterySaver()
                        }
                    }
                )
                ConfigurationToggle(stringResource(R.string.enable_auto_tunnel),
                    enabled = !settings.isAlwaysOnVpnEnabled,
                    checked = settings.isAutoTunnelEnabled,
                    padding = screenPadding,
                    onCheckChanged = {
                        if(!isAllAutoTunnelPermissionsEnabled()) {
                            val message = if(viewModel.isLocationServicesNeeded()){
                                context.getString(R.string.location_services_required)
                            } else if(!isBackgroundLocationGranted){
                                context.getString(R.string.background_location_required)
                            } else {
                                context.getString(R.string.precise_location_required)
                            }
                            showSnackbarMessage(message)
                        } else scope.launch {
                            viewModel.toggleAutoTunnel()
                        }
                    }
                )
            }

        }
        if(!WireGuardAutoTunnel.isRunningOnAndroidTv(context)) {
            Surface(
                tonalElevation = 2.dp,
                shadowElevation = 2.dp,
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth(fillMaxWidth)
                    .height(IntrinsicSize.Min)
                    .padding(bottom = 180.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.Top,
                    modifier = Modifier.padding(15.dp)
                ) {
                    SectionTitle(title = stringResource(id = R.string.other), padding = screenPadding)
                    ConfigurationToggle(stringResource(R.string.always_on_vpn_support),
                        enabled = !settings.isAutoTunnelEnabled,
                        checked = settings.isAlwaysOnVpnEnabled,
                        padding = screenPadding,
                        onCheckChanged = {
                            scope.launch {
                                viewModel.onToggleAlwaysOnVPN()
                            }
                        }
                    )
                    ConfigurationToggle(stringResource(R.string.enabled_app_shortcuts),
                        enabled = true,
                        checked = settings.isShortcutsEnabled,
                        padding = screenPadding,
                        onCheckChanged = {
                            scope.launch {
                                viewModel.onToggleShortcutsEnabled()
                            }
                        }
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 5.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        TextButton(
                            enabled = !didExportFiles,
                            onClick = {
                                showAuthPrompt = true
                            }) {
                            Text(stringResource(R.string.export_configs))
                        }
                    }
                }
            }
        }
        if(WireGuardAutoTunnel.isRunningOnAndroidTv(context)) {
            Spacer(modifier = Modifier.weight(.17f))
        }
    }
}


