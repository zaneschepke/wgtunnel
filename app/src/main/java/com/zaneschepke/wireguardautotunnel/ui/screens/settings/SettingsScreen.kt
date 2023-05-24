package com.zaneschepke.wireguardautotunnel.ui.screens.settings

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.LocationOff
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalContext
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
import com.zaneschepke.wireguardautotunnel.service.tunnel.model.TunnelConfig
import com.zaneschepke.wireguardautotunnel.ui.Routes
import com.zaneschepke.wireguardautotunnel.ui.common.ClickableIconButton
import com.zaneschepke.wireguardautotunnel.ui.common.PermissionRequestFailedScreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class,
    ExperimentalLayoutApi::class
)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    padding: PaddingValues,
    navController: NavController,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val trustedSSIDs by viewModel.trustedSSIDs.collectAsStateWithLifecycle()
    val tunnels by viewModel.tunnels.collectAsStateWithLifecycle(mutableListOf())
    val backgroundLocationState =
        rememberPermissionState(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
    var currentText by remember { mutableStateOf("") }

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
    if(!backgroundLocationState.status.isGranted) {
        Column(horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)) {
            Icon(Icons.Rounded.LocationOff, contentDescription = "Map", modifier = Modifier.padding(30.dp).size(128.dp))
            Text(stringResource(R.string.prominent_background_location_title), textAlign = TextAlign.Center, modifier = Modifier.padding(30.dp), fontSize = 20.sp)
            Text(stringResource(R.string.prominent_background_location_message), textAlign = TextAlign.Center, modifier = Modifier.padding(30.dp), fontSize = 15.sp)
            //Spacer(modifier = Modifier.weight(1f))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(30.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(onClick = {
                    navController.navigate(Routes.Main.name)
                }) {
                    Text("No thanks")
                }
                Button(onClick = {
                    scope.launch {
                        val intentSettings =
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        intentSettings.data =
                            Uri.fromParts("package", context.packageName, null)
                        context.startActivity(intentSettings)
                    }
                }) {
                    Text("Turn on")
                }
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

    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Top,
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(stringResource(R.string.enable_auto_tunnel))
            Switch(
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
            modifier = Modifier.padding(15.dp, bottom = 5.dp, top = 5.dp)
        )
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = {
                if(!settings.isAutoTunnelEnabled) {
                expanded = !expanded }},
            modifier = Modifier.padding(start = 15.dp, top = 5.dp, bottom = 10.dp),
        ) {
            TextField(
                enabled = !settings.isAutoTunnelEnabled,
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
            modifier = Modifier.padding(15.dp, bottom = 5.dp, top = 5.dp)
        )
        FlowRow(
            modifier = Modifier.padding(15.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            trustedSSIDs.forEach { ssid ->
                ClickableIconButton(onIconClick = {
                    scope.launch {
                        viewModel.onDeleteTrustedSSID(ssid)
                    }
                }, text = ssid, icon = Icons.Filled.Close, enabled = !settings.isAutoTunnelEnabled)
            }
        }
        OutlinedTextField(
            enabled = !settings.isAutoTunnelEnabled,
            value = currentText,
            onValueChange = { currentText = it },
            label = { Text(stringResource(R.string.add_trusted_ssid)) },
            modifier = Modifier.padding(start = 15.dp, top = 5.dp),
            maxLines = 1,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    scope.launch {
                        if (currentText.isNotEmpty()) {
                            viewModel.onSaveTrustedSSID(currentText)
                            currentText = ""
                        }
                    }
                }
            ),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(stringResource(R.string.tunnel_mobile_data))
            Switch(
                enabled = !settings.isAutoTunnelEnabled,
                checked = settings.isTunnelOnMobileDataEnabled,
                onCheckedChange = {
                    scope.launch {
                        viewModel.onToggleTunnelOnMobileData()
                    }
                }
            )
        }
    }
}


