package com.zaneschepke.wireguardautotunnel.ui.screens.config

import android.annotation.SuppressLint
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.google.accompanist.drawablepainter.DrawablePainter
import com.zaneschepke.wireguardautotunnel.Constants
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.WireGuardAutoTunnel
import com.zaneschepke.wireguardautotunnel.ui.Routes
import com.zaneschepke.wireguardautotunnel.ui.common.SearchBar
import com.zaneschepke.wireguardautotunnel.ui.common.config.ConfigurationTextBox
import com.zaneschepke.wireguardautotunnel.ui.common.prompt.AuthorizationPrompt
import com.zaneschepke.wireguardautotunnel.ui.common.text.SectionTitle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(
    ExperimentalComposeUiApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class
)
@Composable
fun ConfigScreen(
    viewModel: ConfigViewModel = hiltViewModel(),
    focusRequester: FocusRequester,
    navController: NavController,
    showSnackbarMessage: (String) -> Unit,
    id: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboardManager: ClipboardManager = LocalClipboardManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val tunnel by viewModel.tunnel.collectAsStateWithLifecycle(null)
    val tunnelName = viewModel.tunnelName.collectAsStateWithLifecycle()
    val packages by viewModel.packages.collectAsStateWithLifecycle()
    val checkedPackages by viewModel.checkedPackages.collectAsStateWithLifecycle()
    val include by viewModel.include.collectAsStateWithLifecycle()
    val isAllApplicationsEnabled by viewModel.isAllApplicationsEnabled.collectAsStateWithLifecycle()
    val proxyPeers by viewModel.proxyPeers.collectAsStateWithLifecycle()
    val proxyInterface by viewModel.interfaceProxy.collectAsStateWithLifecycle()
    var showApplicationsDialog by remember { mutableStateOf(false) }
    var showAuthPrompt by remember { mutableStateOf(false) }
    var isAuthenticated by remember { mutableStateOf(false) }
    val baseTextBoxModifier =
        Modifier.onFocusChanged {
            if (WireGuardAutoTunnel.isRunningOnAndroidTv(context)) {
                keyboardController?.hide()
            }
        }

    val keyboardActions =
        KeyboardActions(
            onDone = {
                keyboardController?.hide()
            }
        )

    val keyboardOptions =
        KeyboardOptions(
            capitalization = KeyboardCapitalization.None,
            imeAction = ImeAction.Done
        )

    val fillMaxHeight = .85f
    val fillMaxWidth = .85f
    val screenPadding = 5.dp

    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            try {
                viewModel.onScreenLoad(id)
            } catch (e: Exception) {
                showSnackbarMessage(e.message!!)
                navController.navigate(Routes.Main.name)
            }
        }
    }

    val applicationButtonText = {
        "Tunneling apps: " +
                if (isAllApplicationsEnabled) {
                    "all"
                } else {
                    "${checkedPackages.size} " + (if (include) "included" else "excluded")
                }
    }

    if (showAuthPrompt) {
        AuthorizationPrompt(
            onSuccess = {
                showAuthPrompt = false
                isAuthenticated = true
            },
            onError = { error ->
                showSnackbarMessage(error)
                showAuthPrompt = false
            },
            onFailure = {
                showAuthPrompt = false
                showSnackbarMessage(context.getString(R.string.authentication_failed))
            }
        )
    }

    if (showApplicationsDialog) {
        val sortedPackages =
            remember(packages) {
                packages.sortedBy { viewModel.getPackageLabel(it) }
            }
        AlertDialog(onDismissRequest = {
            showApplicationsDialog = false
        }) {
            Surface(
                tonalElevation = 2.dp,
                shadowElevation = 2.dp,
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier =
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(if (isAllApplicationsEnabled) 1 / 5f else 4 / 5f)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(stringResource(id = R.string.tunnel_all))
                        Switch(
                            checked = isAllApplicationsEnabled,
                            onCheckedChange = {
                                viewModel.onAllApplicationsChange(it)
                            }
                        )
                    }
                    if (!isAllApplicationsEnabled) {
                        Row(
                            modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = 20.dp,
                                    vertical = 7.dp
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(stringResource(id = R.string.include))
                                Checkbox(
                                    checked = include,
                                    onCheckedChange = {
                                        viewModel.onIncludeChange(!include)
                                    }
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(stringResource(id = R.string.exclude))
                                Checkbox(
                                    checked = !include,
                                    onCheckedChange = {
                                        viewModel.onIncludeChange(!include)
                                    }
                                )
                            }
                        }
                        Row(
                            modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = 20.dp,
                                    vertical = 7.dp
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            SearchBar(viewModel::emitQueriedPackages)
                        }
                        Spacer(Modifier.padding(5.dp))
                        LazyColumn(
                            horizontalAlignment = Alignment.Start,
                            verticalArrangement = Arrangement.Top,
                            modifier =
                            Modifier
                                .fillMaxHeight(4 / 5f)
                        ) {
                            items(
                                sortedPackages,
                                key = { it.packageName }
                            ) { pack ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier =
                                    Modifier
                                        .fillMaxSize()
                                        .padding(5.dp)
                                ) {
                                    Row(
                                        modifier =
                                        Modifier.fillMaxWidth(
                                            fillMaxWidth
                                        )
                                    ) {
                                        val drawable =
                                            pack.applicationInfo?.loadIcon(
                                                context.packageManager
                                            )
                                        if (drawable != null) {
                                            Image(
                                                painter =
                                                DrawablePainter(
                                                    drawable
                                                ),
                                                stringResource(id = R.string.icon),
                                                modifier =
                                                Modifier.size(
                                                    50.dp,
                                                    50.dp
                                                )
                                            )
                                        } else {
                                            Icon(
                                                Icons.Rounded.Android,
                                                stringResource(id = R.string.edit),
                                                modifier =
                                                Modifier.size(
                                                    50.dp,
                                                    50.dp
                                                )
                                            )
                                        }
                                        Text(
                                            viewModel.getPackageLabel(pack),
                                            modifier = Modifier.padding(5.dp)
                                        )
                                    }
                                    Checkbox(
                                        modifier = Modifier.fillMaxSize(),
                                        checked = (checkedPackages.contains(pack.packageName)),
                                        onCheckedChange = {
                                            if (it) {
                                                viewModel.onAddCheckedPackage(
                                                    pack.packageName
                                                )
                                            } else {
                                                viewModel.onRemoveCheckedPackage(
                                                    pack.packageName
                                                )
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(top = 5.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        TextButton(
                            onClick = {
                                showApplicationsDialog = false
                            }
                        ) {
                            Text(stringResource(R.string.done))
                        }
                    }
                }
            }
        }
    }

    if (tunnel != null) {
        Scaffold(
            floatingActionButtonPosition = FabPosition.End,
            floatingActionButton = {
                val secondaryColor = MaterialTheme.colorScheme.secondary
                val hoverColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
                var fobColor by remember { mutableStateOf(secondaryColor) }
                FloatingActionButton(
                    modifier =
                    Modifier.padding(bottom = 90.dp).onFocusChanged {
                        if (WireGuardAutoTunnel.isRunningOnAndroidTv(context)) {
                            fobColor = if (it.isFocused) hoverColor else secondaryColor
                        }
                    },
                    onClick = {
                        scope.launch {
                            try {
                                viewModel.onSaveAllChanges()
                                navController.navigate(Routes.Main.name)
                                showSnackbarMessage(
                                    context.resources.getString(R.string.config_changes_saved)
                                )
                            } catch (e: Exception) {
                                Timber.e(e.message)
                                showSnackbarMessage(e.message!!)
                            }
                        }
                    },
                    containerColor = fobColor,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Save,
                        contentDescription = stringResource(id = R.string.save_changes),
                        tint = Color.DarkGray
                    )
                }
            }
        ) {
            Column {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top,
                    modifier =
                    Modifier
                        .verticalScroll(rememberScrollState())
                        .weight(1f, true)
                        .fillMaxSize()
                ) {
                    Surface(
                        tonalElevation = 2.dp,
                        shadowElevation = 2.dp,
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier =
                        (
                                if (WireGuardAutoTunnel.isRunningOnAndroidTv(context)) {
                                    Modifier
                                        .fillMaxHeight(fillMaxHeight)
                                        .fillMaxWidth(fillMaxWidth)
                                } else {
                                    Modifier.fillMaxWidth(fillMaxWidth)
                                }
                                ).padding(
                                top = 50.dp,
                                bottom = 10.dp
                            )
                    ) {
                        Column(
                            horizontalAlignment = Alignment.Start,
                            verticalArrangement = Arrangement.Top,
                            modifier = Modifier.padding(15.dp).focusGroup()
                        ) {
                            SectionTitle(
                                stringResource(R.string.interface_),
                                padding = screenPadding
                            )
                            ConfigurationTextBox(
                                value = tunnelName.value,
                                onValueChange = { value ->
                                    viewModel.onTunnelNameChange(value)
                                },
                                keyboardActions = keyboardActions,
                                label = stringResource(R.string.name),
                                hint = stringResource(R.string.tunnel_name).lowercase(),
                                modifier = baseTextBoxModifier.fillMaxWidth().focusRequester(
                                    focusRequester
                                )
                            )
                            OutlinedTextField(
                                modifier =
                                baseTextBoxModifier.fillMaxWidth().clickable {
                                    showAuthPrompt = true
                                },
                                value = proxyInterface.privateKey,
                                visualTransformation = if ((id == Constants.MANUAL_TUNNEL_CONFIG_ID) || isAuthenticated) VisualTransformation.None else PasswordVisualTransformation(),
                                enabled = (id == Constants.MANUAL_TUNNEL_CONFIG_ID) || isAuthenticated,
                                onValueChange = { value ->
                                    viewModel.onPrivateKeyChange(value)
                                },
                                trailingIcon = {
                                    IconButton(
                                        modifier = Modifier.focusRequester(FocusRequester.Default),
                                        onClick = {
                                            viewModel.generateKeyPair()
                                        }
                                    ) {
                                        Icon(
                                            Icons.Rounded.Refresh,
                                            stringResource(R.string.rotate_keys),
                                            tint = Color.White
                                        )
                                    }
                                },
                                label = { Text(stringResource(R.string.private_key)) },
                                singleLine = true,
                                placeholder = { Text(stringResource(R.string.base64_key)) },
                                keyboardOptions = keyboardOptions,
                                keyboardActions = keyboardActions
                            )
                            OutlinedTextField(
                                modifier = baseTextBoxModifier.fillMaxWidth().focusRequester(
                                    FocusRequester.Default
                                ),
                                value = proxyInterface.publicKey,
                                enabled = false,
                                onValueChange = {},
                                trailingIcon = {
                                    IconButton(
                                        modifier = Modifier.focusRequester(FocusRequester.Default),
                                        onClick = {
                                            clipboardManager.setText(
                                                AnnotatedString(proxyInterface.publicKey)
                                            )
                                        }
                                    ) {
                                        Icon(
                                            Icons.Rounded.ContentCopy,
                                            stringResource(R.string.copy_public_key),
                                            tint = Color.White
                                        )
                                    }
                                },
                                label = { Text(stringResource(R.string.public_key)) },
                                singleLine = true,
                                placeholder = { Text(stringResource(R.string.base64_key)) },
                                keyboardOptions = keyboardOptions,
                                keyboardActions = keyboardActions
                            )
                            Row(modifier = Modifier.fillMaxWidth()) {
                                ConfigurationTextBox(
                                    value = proxyInterface.addresses,
                                    onValueChange = { value ->
                                        viewModel.onAddressesChanged(value)
                                    },
                                    keyboardActions = keyboardActions,
                                    label = stringResource(R.string.addresses),
                                    hint = stringResource(R.string.comma_separated_list),
                                    modifier =
                                    baseTextBoxModifier
                                        .fillMaxWidth(3 / 5f)
                                        .padding(end = 5.dp)
                                )
                                ConfigurationTextBox(
                                    value = proxyInterface.listenPort,
                                    onValueChange = { value -> viewModel.onListenPortChanged(value) },
                                    keyboardActions = keyboardActions,
                                    label = stringResource(R.string.listen_port),
                                    hint = stringResource(R.string.random),
                                    modifier = baseTextBoxModifier.width(IntrinsicSize.Min)
                                )
                            }
                            Row(modifier = Modifier.fillMaxWidth()) {
                                ConfigurationTextBox(
                                    value = proxyInterface.dnsServers,
                                    onValueChange = { value -> viewModel.onDnsServersChanged(value) },
                                    keyboardActions = keyboardActions,
                                    label = stringResource(R.string.dns_servers),
                                    hint = stringResource(R.string.comma_separated_list),
                                    modifier =
                                    baseTextBoxModifier
                                        .fillMaxWidth(3 / 5f)
                                        .padding(end = 5.dp)
                                )
                                ConfigurationTextBox(
                                    value = proxyInterface.mtu,
                                    onValueChange = { value -> viewModel.onMtuChanged(value) },
                                    keyboardActions = keyboardActions,
                                    label = stringResource(R.string.mtu),
                                    hint = stringResource(R.string.auto),
                                    modifier = baseTextBoxModifier.width(IntrinsicSize.Min)
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier =
                                Modifier
                                    .fillMaxSize()
                                    .padding(top = 5.dp),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                TextButton(
                                    onClick = {
                                        showApplicationsDialog = true
                                    }
                                ) {
                                    Text(applicationButtonText())
                                }
                            }
                        }
                    }
                    proxyPeers.forEachIndexed { index, peer ->
                        Surface(
                            tonalElevation = 2.dp,
                            shadowElevation = 2.dp,
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier =
                            (
                                    if (WireGuardAutoTunnel.isRunningOnAndroidTv(context)) {
                                        Modifier
                                            .fillMaxHeight(fillMaxHeight)
                                            .fillMaxWidth(fillMaxWidth)
                                    } else {
                                        Modifier.fillMaxWidth(fillMaxWidth)
                                    }
                                    ).padding(
                                    top = 10.dp,
                                    bottom = 10.dp
                                )
                        ) {
                            Column(
                                horizontalAlignment = Alignment.Start,
                                verticalArrangement = Arrangement.Top,
                                modifier =
                                Modifier
                                    .padding(horizontal = 15.dp)
                                    .padding(bottom = 10.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 5.dp)
                                ) {
                                    SectionTitle(
                                        stringResource(R.string.peer),
                                        padding = screenPadding
                                    )
                                    IconButton(
                                        onClick = {
                                            viewModel.onDeletePeer(index)
                                        }
                                    ) {
                                        Icon(Icons.Rounded.Delete, stringResource(R.string.delete))
                                    }
                                }

                                ConfigurationTextBox(
                                    value = peer.publicKey,
                                    onValueChange = { value ->
                                        viewModel.onPeerPublicKeyChange(
                                            index,
                                            value
                                        )
                                    },
                                    keyboardActions = keyboardActions,
                                    label = stringResource(R.string.public_key),
                                    hint = stringResource(R.string.base64_key),
                                    modifier = baseTextBoxModifier.fillMaxWidth()
                                )
                                ConfigurationTextBox(
                                    value = peer.preSharedKey,
                                    onValueChange = { value ->
                                        viewModel.onPreSharedKeyChange(
                                            index,
                                            value
                                        )
                                    },
                                    keyboardActions = keyboardActions,
                                    label = stringResource(R.string.preshared_key),
                                    hint = stringResource(R.string.optional),
                                    modifier = baseTextBoxModifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    modifier = baseTextBoxModifier.fillMaxWidth(),
                                    value = peer.persistentKeepalive,
                                    enabled = true,
                                    onValueChange = { value ->
                                        viewModel.onPersistentKeepaliveChanged(index, value)
                                    },
                                    trailingIcon = {
                                        Text(
                                            stringResource(R.string.seconds),
                                            modifier = Modifier.padding(end = 10.dp)
                                        )
                                    },
                                    label = { Text(stringResource(R.string.persistent_keepalive)) },
                                    singleLine = true,
                                    placeholder = {
                                        Text(stringResource(R.string.optional_no_recommend))
                                    },
                                    keyboardOptions = keyboardOptions,
                                    keyboardActions = keyboardActions
                                )
                                ConfigurationTextBox(
                                    value = peer.endpoint,
                                    onValueChange = { value ->
                                        viewModel.onEndpointChange(
                                            index,
                                            value
                                        )
                                    },
                                    keyboardActions = keyboardActions,
                                    label = stringResource(R.string.endpoint),
                                    hint = stringResource(R.string.endpoint).lowercase(),
                                    modifier = baseTextBoxModifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    modifier = baseTextBoxModifier.fillMaxWidth(),
                                    value = peer.allowedIps,
                                    enabled = true,
                                    onValueChange = { value ->
                                        viewModel.onAllowedIpsChange(
                                            index,
                                            value
                                        )
                                    },
                                    label = { Text(stringResource(R.string.allowed_ips)) },
                                    singleLine = true,
                                    placeholder = {
                                        Text(stringResource(R.string.comma_separated_list))
                                    },
                                    keyboardOptions = keyboardOptions,
                                    keyboardActions = keyboardActions
                                )
                            }
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(bottom = 140.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            TextButton(
                                onClick = {
                                    viewModel.addEmptyPeer()
                                }
                            ) {
                                Text(stringResource(R.string.add_peer))
                            }
                        }
                    }
                }
                if (WireGuardAutoTunnel.isRunningOnAndroidTv(context)) {
                    Spacer(modifier = Modifier.weight(.17f))
                }
            }
        }
    }
}
