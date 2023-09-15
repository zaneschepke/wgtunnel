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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.rounded.LocationOff
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.WireGuardAutoTunnel
import com.zaneschepke.wireguardautotunnel.repository.model.TunnelConfig
import com.zaneschepke.wireguardautotunnel.ui.Routes
import com.zaneschepke.wireguardautotunnel.ui.common.ClickableIconButton
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class,
    ExperimentalLayoutApi::class
)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    padding: PaddingValues,
    navController: NavController,
    focusRequester: FocusRequester,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val interactionSource = remember { MutableInteractionSource() }

    var expanded by remember { mutableStateOf(false) }
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val trustedSSIDs by viewModel.trustedSSIDs.collectAsStateWithLifecycle()
    val tunnels by viewModel.tunnels.collectAsStateWithLifecycle(mutableListOf())
    val fineLocationState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    var currentText by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()
    var isLocationServicesEnabled by remember { mutableStateOf(viewModel.checkLocationServicesEnabled())}

    LaunchedEffect(viewState) {
        if (viewState.showSnackbarMessage) {
            val result = snackbarHostState.showSnackbar(
                message = viewState.snackbarMessage,
                actionLabel = viewState.snackbarActionText,
                duration = SnackbarDuration.Long,
            )
            when (result) {
                SnackbarResult.ActionPerformed -> viewState.onSnackbarActionClick
                SnackbarResult.Dismissed -> viewState.onSnackbarActionClick
            }
        }
    }

    fun saveTrustedSSID() {
        if (currentText.isNotEmpty()) {
            scope.launch {
                viewModel.onSaveTrustedSSID(currentText)
                currentText = ""
            }
        }
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
            Column(horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top,
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(padding)) {
                Icon(Icons.Rounded.LocationOff, contentDescription = stringResource(id = R.string.map), modifier = Modifier
                    .padding(30.dp)
                    .size(128.dp))
                Text(stringResource(R.string.prominent_background_location_title), textAlign = TextAlign.Center, modifier = Modifier.padding(30.dp), fontSize = 20.sp)
                Text(stringResource(R.string.prominent_background_location_message), textAlign = TextAlign.Center, modifier = Modifier.padding(30.dp), fontSize = 15.sp)
                Row(
                    modifier = if(WireGuardAutoTunnel.isRunningOnAndroidTv(context)) Modifier
                        .fillMaxWidth()
                        .padding(10.dp) else Modifier
                        .fillMaxWidth()
                        .padding(30.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(onClick = {
                        navController.navigate(Routes.Main.name)
                    }) {
                        Text(stringResource(id = R.string.no_thanks))
                    }
                    Button(modifier = Modifier.focusRequester(focusRequester), onClick = {
                        openSettings()
                    }) {
                        Text(stringResource(id = R.string.turn_on))
                    }
                }
            }
            return
        }
    }

    if(!fineLocationState.status.isGranted) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Text(
                stringResource(id = R.string.precise_location_message),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(15.dp),
                fontStyle = FontStyle.Italic
            )
            Button(modifier = Modifier.focusRequester(focusRequester),onClick = {
                fineLocationState.launchPermissionRequest()
            }) {
                Text(stringResource(id = R.string.request))
            }

        }
        return
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
    if(!isLocationServicesEnabled && Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Text(
                stringResource(id = R.string.location_services_not_detected),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(15.dp),
                fontStyle = FontStyle.Italic
            )
            Button(modifier = Modifier.focusRequester(focusRequester), onClick = {
                val locationServicesEnabled = viewModel.checkLocationServicesEnabled()
                isLocationServicesEnabled = locationServicesEnabled
                if(!locationServicesEnabled) {
                    scope.launch {
                        viewModel.showSnackBarMessage(context.getString(R.string.detecting_location_services_disabled))
                    }
                }
            }) {
                Text(stringResource(id = R.string.check_again))
            }
        }
        return
    }
    val screenPadding = if(WireGuardAutoTunnel.isRunningOnAndroidTv(context)) 5.dp else 15.dp
    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Top,
        modifier = if(WireGuardAutoTunnel.isRunningOnAndroidTv(context)) Modifier
            .fillMaxHeight(.85f)
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .clickable(indication = null, interactionSource = interactionSource) {
                focusManager.clearFocus()
            }
            .padding(padding) else Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .clickable(indication = null, interactionSource = interactionSource) {
                focusManager.clearFocus()
            }
            .padding(padding)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(screenPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(stringResource(R.string.enable_auto_tunnel))
            Switch(
                modifier = Modifier.focusRequester(focusRequester),
                enabled = !settings.isAlwaysOnVpnEnabled,
                checked = settings.isAutoTunnelEnabled,
                onCheckedChange = {
                    scope.launch {
                        viewModel.toggleAutoTunnel()
                    }
                }
            )
        }
        Text(
            stringResource(id = R.string.select_tunnel),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(screenPadding, bottom = 5.dp, top = 5.dp)
        )
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = {
                if(!(settings.isAutoTunnelEnabled || settings.isAlwaysOnVpnEnabled)) {
                expanded = !expanded }},
            modifier = Modifier.padding(start = 15.dp, top = 5.dp, bottom = 10.dp).clickable {
                  expanded = !expanded
            },
        ) {
            TextField(
                enabled = !(settings.isAutoTunnelEnabled || settings.isAlwaysOnVpnEnabled),
                value = settings.defaultTunnel?.let {
                    TunnelConfig.from(it).name }
                    ?: "",
                readOnly = true,
                modifier = Modifier.menuAnchor(),
                label = { Text(stringResource(R.string.tunnels)) },
                onValueChange = { },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(
                        expanded = expanded
                    )
                }
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = {
                    expanded = false
                }
            ) {
                tunnels.forEach() { tunnel ->
                    DropdownMenuItem(
                        onClick = {
                            scope.launch {
                                viewModel.onDefaultTunnelSelected(tunnel)
                            }
                            expanded = false
                        },
                        text = { Text(text = tunnel.name) }
                    )
                }
            }
        }
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
                ClickableIconButton(onIconClick = {
                    scope.launch {
                        viewModel.onDeleteTrustedSSID(ssid)
                    }
                }, text = ssid, icon = Icons.Filled.Close, enabled = !(settings.isAutoTunnelEnabled || settings.isAlwaysOnVpnEnabled))
            }
        }
        OutlinedTextField(
            enabled = !(settings.isAutoTunnelEnabled || settings.isAlwaysOnVpnEnabled),
            value = currentText,
            onValueChange = { currentText = it },
            label = { Text(stringResource(R.string.add_trusted_ssid)) },
            modifier = Modifier.padding(start = screenPadding, top = 5.dp),
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
                        tint = if(currentText == "") Color.Transparent else Color.Green
                    )
                }
            },
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(screenPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(stringResource(R.string.tunnel_mobile_data))
            Switch(
                enabled = !(settings.isAutoTunnelEnabled || settings.isAlwaysOnVpnEnabled),
                checked = settings.isTunnelOnMobileDataEnabled,
                onCheckedChange = {
                    scope.launch {
                        viewModel.onToggleTunnelOnMobileData()
                    }
                }
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(screenPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(stringResource(R.string.always_on_vpn_support))
            Switch(
                enabled = !settings.isAutoTunnelEnabled,
                checked = settings.isAlwaysOnVpnEnabled,
                onCheckedChange = {
                    scope.launch {
                        viewModel.onToggleAlwaysOnVPN()
                    }
                }
            )
        }
    }
}


