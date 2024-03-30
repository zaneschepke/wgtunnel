package com.zaneschepke.wireguardautotunnel.ui.screens.options

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.WireGuardAutoTunnel
import com.zaneschepke.wireguardautotunnel.ui.AppViewModel
import com.zaneschepke.wireguardautotunnel.ui.Screen
import com.zaneschepke.wireguardautotunnel.ui.common.ClickableIconButton
import com.zaneschepke.wireguardautotunnel.ui.common.config.ConfigurationToggle
import com.zaneschepke.wireguardautotunnel.ui.common.text.SectionTitle
import com.zaneschepke.wireguardautotunnel.util.Constants
import com.zaneschepke.wireguardautotunnel.util.Result
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OptionsScreen(
    optionsViewModel: OptionsViewModel = hiltViewModel(),
    navController: NavController,
    appViewModel: AppViewModel,
    focusRequester: FocusRequester,
    tunnelId: String
) {
    val scrollState = rememberScrollState()
    val uiState by optionsViewModel.uiState.collectAsStateWithLifecycle()
    val interactionSource = remember { MutableInteractionSource() }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val screenPadding = 5.dp
    val fillMaxWidth = .85f

    var currentText by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        optionsViewModel.init(tunnelId)
        if (WireGuardAutoTunnel.isRunningOnAndroidTv()) {
            delay(Constants.FOCUS_REQUEST_DELAY)
            focusRequester.requestFocus()
        }
    }

    fun saveTrustedSSID() {
        if (currentText.isNotEmpty()) {
            scope.launch {
                optionsViewModel.onSaveRunSSID(currentText).let {
                    when (it) {
                        is Result.Success -> currentText = ""
                        is Result.Error -> appViewModel.showSnackbarMessage(it.error.message)
                    }
                }
            }
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
        modifier =
        Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .clickable(
                indication = null,
                interactionSource = interactionSource,
            ) {
                focusManager.clearFocus()
            },
    ) {
        Surface(
            tonalElevation = 2.dp,
            shadowElevation = 2.dp,
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier =
            (if (WireGuardAutoTunnel.isRunningOnAndroidTv()) {
                Modifier
                    .height(IntrinsicSize.Min)
                    .fillMaxWidth(fillMaxWidth)
                    .padding(top = 10.dp)
            } else {
                Modifier
                    .fillMaxWidth(fillMaxWidth)
                    .padding(top = 20.dp)
            })
                .padding(bottom = 10.dp),
        ) {
            Column(
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Top,
                modifier = Modifier.padding(15.dp),
            ) {
                SectionTitle(
                    title = stringResource(id = R.string.general),
                    padding = screenPadding,
                )
                ConfigurationToggle(
                    stringResource(R.string.set_primary_tunnel),
                    enabled = true,
                    checked = uiState.isDefaultTunnel,
                    modifier = Modifier
                        .focusRequester(focusRequester),
                    padding = screenPadding,
                    onCheckChanged = { optionsViewModel.onTogglePrimaryTunnel() },
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 5.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    TextButton(
                        onClick = {
                            navController.navigate(
                                "${Screen.Config.route}/${tunnelId}",
                            )
                        },
                    ) {
                        Text(stringResource(R.string.edit_tunnel))
                    }
                }
            }
        }
        Surface(
            tonalElevation = 2.dp,
            shadowElevation = 2.dp,
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier =
            (if (WireGuardAutoTunnel.isRunningOnAndroidTv()) {
                Modifier
                    .height(IntrinsicSize.Min)
                    .fillMaxWidth(fillMaxWidth)
                    .padding(top = 10.dp)
            } else {
                Modifier
                    .fillMaxWidth(fillMaxWidth)
                    .padding(top = 20.dp)
            })
                .padding(bottom = 10.dp),
        ) {
            Column(
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Top,
                modifier = Modifier.padding(15.dp),
            ) {
                SectionTitle(
                    title = stringResource(id = R.string.auto_tunneling),
                    padding = screenPadding,
                )
                ConfigurationToggle(
                    stringResource(R.string.mobile_data_tunnel),
                    enabled = true,
                    checked = uiState.tunnel?.isMobileDataTunnel == true,
                    padding = screenPadding,
                    onCheckChanged = { optionsViewModel.onToggleIsMobileDataTunnel() },
                )
                Column {
                    FlowRow(
                        modifier = Modifier
                            .padding(screenPadding)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        uiState.tunnel?.tunnelNetworks?.forEach { ssid ->
                            ClickableIconButton(
                                onClick = {
                                    if (WireGuardAutoTunnel.isRunningOnAndroidTv()) {
                                        focusRequester.requestFocus()
                                        optionsViewModel.onDeleteRunSSID(ssid)
                                    }
                                },
                                onIconClick = {
                                    if (WireGuardAutoTunnel.isRunningOnAndroidTv()) focusRequester.requestFocus()
                                    optionsViewModel.onDeleteRunSSID(ssid)

                                },
                                text = ssid,
                                icon = Icons.Filled.Close,
                                enabled = true,
                            )
                        }
                        if (uiState.tunnel == null || uiState.tunnel?.tunnelNetworks?.isEmpty() == true) {
                            Text(
                                stringResource(R.string.no_wifi_names_configured),
                                fontStyle = FontStyle.Italic,
                                color = Color.Gray,
                            )
                        }
                    }
                    OutlinedTextField(
                        enabled = true,
                        value = currentText,
                        onValueChange = { currentText = it },
                        label = { Text(stringResource(id = R.string.use_tunnel_on_wifi_name)) },
                        modifier =
                        Modifier
                            .padding(
                                start = screenPadding,
                                top = 5.dp,
                                bottom = 10.dp,
                            ),
                        maxLines = 1,
                        keyboardOptions =
                        KeyboardOptions(
                            capitalization = KeyboardCapitalization.None,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(onDone = { saveTrustedSSID() }),
                        trailingIcon = {
                            if (currentText != "") {
                                IconButton(onClick = { saveTrustedSSID() }) {
                                    Icon(
                                        imageVector = Icons.Outlined.Add,
                                        contentDescription =
                                        if (currentText == "") {
                                            stringResource(
                                                id =
                                                R.string
                                                    .trusted_ssid_empty_description,
                                            )
                                        } else {
                                            stringResource(
                                                id =
                                                R.string
                                                    .trusted_ssid_value_description,
                                            )
                                        },
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                        },
                    )
                }
            }
        }
    }
}
