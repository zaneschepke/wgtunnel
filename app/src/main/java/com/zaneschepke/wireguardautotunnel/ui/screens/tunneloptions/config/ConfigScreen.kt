package com.zaneschepke.wireguardautotunnel.ui.screens.tunneloptions.config

import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wireguard.config.Config
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.AppUiState
import com.zaneschepke.wireguardautotunnel.ui.common.config.ConfigurationTextBox
import com.zaneschepke.wireguardautotunnel.ui.common.config.ConfigurationToggle
import com.zaneschepke.wireguardautotunnel.ui.common.navigation.LocalNavController
import com.zaneschepke.wireguardautotunnel.ui.common.navigation.TopNavBar
import com.zaneschepke.wireguardautotunnel.ui.common.prompt.AuthorizationPrompt
import com.zaneschepke.wireguardautotunnel.ui.common.snackbar.SnackbarController
import com.zaneschepke.wireguardautotunnel.ui.common.text.SectionTitle
import com.zaneschepke.wireguardautotunnel.ui.screens.tunneloptions.config.model.InterfaceProxy
import com.zaneschepke.wireguardautotunnel.ui.screens.tunneloptions.config.model.PeerProxy
import com.zaneschepke.wireguardautotunnel.util.Constants
import com.zaneschepke.wireguardautotunnel.util.extensions.isRunningOnTv
import com.zaneschepke.wireguardautotunnel.util.extensions.scaledHeight
import timber.log.Timber

@Composable
fun ConfigScreen(appUiState : AppUiState, tunnelId: Int, viewModel: ConfigViewModel = hiltViewModel()) {

	val tunnelConfig by remember { derivedStateOf {
		appUiState.tunnels.first { it.id == tunnelId }
	} }

	val configPair by remember { derivedStateOf {
		Pair(tunnelConfig.name,tunnelConfig.toAmConfig()) }
	}

	var tunnelName by remember {
		mutableStateOf(configPair.first)
	}

	var interfaceState by remember {
		mutableStateOf(InterfaceProxy.from(configPair.second.`interface`))
	}

	var showAmneziaValues by remember {
		mutableStateOf(configPair.second.`interface`.junkPacketCount.isPresent)
	}

	val peersState = remember {
		mutableStateListOf<PeerProxy>().apply {
			addAll(configPair.second.peers.map { PeerProxy.from(it) })
		}
	}


	val context = LocalContext.current
	val snackbar = SnackbarController.current
	val clipboardManager: ClipboardManager = LocalClipboardManager.current
	val keyboardController = LocalSoftwareKeyboardController.current
	val navController = LocalNavController.current

	var showAuthPrompt by remember { mutableStateOf(false) }
	var isAuthenticated by remember { mutableStateOf(false) }

	LaunchedEffect(Unit) {
		viewModel.cleanUpUninstalledApps(tunnelConfig)
	}

	val keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() })
	val keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)

	val fillMaxHeight = .85f
	val fillMaxWidth = .85f
	val screenPadding = 5.dp


	if (showAuthPrompt) {
		AuthorizationPrompt(
			onSuccess = {
				showAuthPrompt = false
				isAuthenticated = true
			},
			onError = {
				showAuthPrompt = false
				snackbar.showMessage(
					context.getString(R.string.error_authentication_failed),
				)
			},
			onFailure = {
				showAuthPrompt = false
				snackbar.showMessage(
					context.getString(R.string.error_authorization_failed),
				)
			},
		)
	}

	Scaffold(
		topBar = {
			TopNavBar(stringResource(R.string.edit_tunnel))
		},
		floatingActionButtonPosition = FabPosition.End,
		floatingActionButton = {
			FloatingActionButton(
				onClick = {
					runCatching {
						viewModel.saveConfigChanges(
							tunnelConfig.copy(
								name = tunnelName,
								wgQuick = Config.Builder().apply {
									addPeers(peersState.map { it.toWgPeer() })
									setInterface(interfaceState.toWgInterface())
								}.build().toWgQuickString(true),
								amQuick = org.amnezia.awg.config.Config.Builder().apply {
									addPeers(peersState.map { it.toAmPeer() })
									setInterface(interfaceState.toAmInterface())
								}.build().toAwgQuickString(true)
							)
						)
					}.onFailure {
						Timber.e(it)
						snackbar.showMessage(it.message ?: context.getString(R.string.unknown_error))
					}
				},
				containerColor = MaterialTheme.colorScheme.primary,
				shape = RoundedCornerShape(16.dp),
			) {
				Icon(
					imageVector = Icons.Rounded.Save,
					contentDescription = stringResource(id = R.string.save_changes),
					tint = MaterialTheme.colorScheme.background,
				)
			}
		},
	) { padding ->
		Column(Modifier.padding(padding)) {
			Column(
				horizontalAlignment = Alignment.CenterHorizontally,
				verticalArrangement = Arrangement.Top,
				modifier =
				Modifier
					.verticalScroll(rememberScrollState())
					.weight(1f, true)
					.fillMaxSize(),
			) {
				Surface(
					tonalElevation = 2.dp,
					shadowElevation = 2.dp,
					shape = RoundedCornerShape(12.dp),
					color = MaterialTheme.colorScheme.surface,
					modifier =
					(
						if (context.isRunningOnTv()) {
							Modifier
								.fillMaxHeight(fillMaxHeight)
								.fillMaxWidth(fillMaxWidth)
						} else {
							Modifier.fillMaxWidth(fillMaxWidth)
						}
						)
						.padding(bottom = 10.dp.scaledHeight()).padding(top = 24.dp.scaledHeight()),
				) {
					Column(
						horizontalAlignment = Alignment.Start,
						verticalArrangement = Arrangement.Top,
						modifier =
						Modifier
							.padding(15.dp)
							.focusGroup(),
					) {
						SectionTitle(
							stringResource(R.string.interface_),
							padding = screenPadding,
						)
						ConfigurationToggle(
							stringResource(id = R.string.show_amnezia_properties),
							checked = showAmneziaValues,
							onCheckChanged = { showAmneziaValues = it },
						)
						ConfigurationTextBox(
							value = tunnelName,
							onValueChange = { tunnelName = it },
							keyboardActions = keyboardActions,
							label = stringResource(R.string.name),
							hint = stringResource(R.string.tunnel_name).lowercase(),
							modifier =
							Modifier
								.fillMaxWidth(),
						)
						OutlinedTextField(
							modifier =
							Modifier
								.fillMaxWidth()
								.clickable { showAuthPrompt = true },
							value = interfaceState.privateKey,
							visualTransformation =
							if ((tunnelId == Constants.MANUAL_TUNNEL_CONFIG_ID) || isAuthenticated) {
								VisualTransformation.None
							} else {
								PasswordVisualTransformation()
							},
							enabled = (tunnelId == Constants.MANUAL_TUNNEL_CONFIG_ID) || isAuthenticated,
							onValueChange = { interfaceState = interfaceState.copy(privateKey = it) },
							trailingIcon = {
								IconButton(
									modifier = Modifier.focusRequester(FocusRequester.Default),
									onClick = {
										//TODO handle recreate of key
									},
								) {
									Icon(
										Icons.Rounded.Refresh,
										stringResource(R.string.rotate_keys),
										tint = MaterialTheme.colorScheme.onSurface,
									)
								}
							},
							label = { Text(stringResource(R.string.private_key)) },
							singleLine = true,
							placeholder = { Text(stringResource(R.string.base64_key)) },
							keyboardOptions = keyboardOptions,
							keyboardActions = keyboardActions,
						)
						OutlinedTextField(
							modifier =
							Modifier
								.fillMaxWidth()
								.focusRequester(FocusRequester.Default),
							value = interfaceState.publicKey,
							enabled = false,
							onValueChange = {
								interfaceState = interfaceState.copy(publicKey = it)
							},
							trailingIcon = {
								IconButton(
									modifier = Modifier.focusRequester(FocusRequester.Default),
									onClick = {
										clipboardManager.setText(
											AnnotatedString(interfaceState.publicKey),
										)
									},
								) {
									Icon(
										Icons.Rounded.ContentCopy,
										stringResource(R.string.copy_public_key),
										tint = MaterialTheme.colorScheme.onSurface,
									)
								}
							},
							label = { Text(stringResource(R.string.public_key)) },
							singleLine = true,
							placeholder = { Text(stringResource(R.string.base64_key)) },
							keyboardOptions = keyboardOptions,
							keyboardActions = keyboardActions,
						)
						ConfigurationTextBox(
							value = interfaceState.addresses,
							onValueChange = {
								interfaceState = interfaceState.copy(addresses = it)
							},
							keyboardActions = keyboardActions,
							label = stringResource(R.string.addresses),
							hint = stringResource(R.string.comma_separated_list),
							modifier =
							Modifier
								.fillMaxWidth()
								.padding(end = 5.dp),
						)
						ConfigurationTextBox(
							value = interfaceState.listenPort,
							onValueChange = {
								interfaceState = interfaceState.copy(listenPort = it)
							},
							keyboardActions = keyboardActions,
							label = stringResource(R.string.listen_port),
							hint = stringResource(R.string.random),
							modifier = Modifier.fillMaxWidth(),
						)
						Row(modifier = Modifier.fillMaxWidth()) {
							ConfigurationTextBox(
								value = interfaceState.dnsServers,
								onValueChange = {
									interfaceState = interfaceState.copy(dnsServers = it)
								},
								keyboardActions = keyboardActions,
								label = stringResource(R.string.dns_servers),
								hint = stringResource(R.string.comma_separated_list),
								modifier =
								Modifier
									.fillMaxWidth(3 / 5f)
									.padding(end = 5.dp),
							)
							ConfigurationTextBox(
								value = interfaceState.mtu,
								onValueChange = {
									interfaceState = interfaceState.copy(mtu = it)
								},
								keyboardActions = keyboardActions,
								label = stringResource(R.string.mtu),
								hint = stringResource(R.string.auto),
								modifier = Modifier.width(IntrinsicSize.Min),
							)
						}
						if (showAmneziaValues) {
							ConfigurationTextBox(
								value = interfaceState.junkPacketCount,
								onValueChange = {
									interfaceState = interfaceState.copy(junkPacketCount = it)
								},
								keyboardActions = keyboardActions,
								label = stringResource(R.string.junk_packet_count),
								hint = stringResource(R.string.junk_packet_count).lowercase(),
								modifier =
								Modifier
									.fillMaxWidth(),
							)
							ConfigurationTextBox(
								value = interfaceState.junkPacketMinSize,
								onValueChange = {
									interfaceState = interfaceState.copy(junkPacketMinSize = it)
								},
								keyboardActions = keyboardActions,
								label = stringResource(R.string.junk_packet_minimum_size),
								hint =
								stringResource(
									R.string.junk_packet_minimum_size,
								).lowercase(),
								modifier =
								Modifier
									.fillMaxWidth(),
							)
							ConfigurationTextBox(
								value = interfaceState.junkPacketMaxSize,
								onValueChange = {
									interfaceState = interfaceState.copy(junkPacketMaxSize = it)
								},
								keyboardActions = keyboardActions,
								label = stringResource(R.string.junk_packet_maximum_size),
								hint =
								stringResource(
									R.string.junk_packet_maximum_size,
								).lowercase(),
								modifier =
								Modifier
									.fillMaxWidth(),
							)
							ConfigurationTextBox(
								value = interfaceState.initPacketJunkSize,
								onValueChange = {
									interfaceState = interfaceState.copy(initPacketJunkSize = it)
								},
								keyboardActions = keyboardActions,
								label = stringResource(R.string.init_packet_junk_size),
								hint = stringResource(R.string.init_packet_junk_size).lowercase(),
								modifier =
								Modifier
									.fillMaxWidth(),
							)
							ConfigurationTextBox(
								value = interfaceState.responsePacketJunkSize,
								onValueChange = {
									interfaceState = interfaceState.copy(responsePacketJunkSize = it)
								},
								keyboardActions = keyboardActions,
								label = stringResource(R.string.response_packet_junk_size),
								hint =
								stringResource(
									R.string.response_packet_junk_size,
								).lowercase(),
								modifier =
								Modifier
									.fillMaxWidth(),
							)
							ConfigurationTextBox(
								value = interfaceState.initPacketMagicHeader,
								onValueChange = {
									interfaceState = interfaceState.copy(initPacketMagicHeader = it)
								},
								keyboardActions = keyboardActions,
								label = stringResource(R.string.init_packet_magic_header),
								hint =
								stringResource(
									R.string.init_packet_magic_header,
								).lowercase(),
								modifier =
								Modifier
									.fillMaxWidth(),
							)
							ConfigurationTextBox(
								value = interfaceState.responsePacketMagicHeader,
								onValueChange = {
									interfaceState = interfaceState.copy(responsePacketMagicHeader = it)
								},
								keyboardActions = keyboardActions,
								label = stringResource(R.string.response_packet_magic_header),
								hint =
								stringResource(
									R.string.response_packet_magic_header,
								).lowercase(),
								modifier =
								Modifier
									.fillMaxWidth(),
							)
							ConfigurationTextBox(
								value = interfaceState.underloadPacketMagicHeader,
								onValueChange = {
									interfaceState = interfaceState.copy(underloadPacketMagicHeader = it)
								},
								keyboardActions = keyboardActions,
								label = stringResource(R.string.underload_packet_magic_header),
								hint =
								stringResource(
									R.string.underload_packet_magic_header,
								).lowercase(),
								modifier =
								Modifier
									.fillMaxWidth(),
							)
							ConfigurationTextBox(
								value = interfaceState.transportPacketMagicHeader,
								onValueChange = {
									interfaceState = interfaceState.copy(transportPacketMagicHeader = it)
								},
								keyboardActions = keyboardActions,
								label = stringResource(R.string.transport_packet_magic_header),
								hint =
								stringResource(
									R.string.transport_packet_magic_header,
								).lowercase(),
								modifier =
								Modifier
									.fillMaxWidth(),
							)
						}
					}
				}
				peersState.forEachIndexed { index, peer ->
					Surface(
						tonalElevation = 2.dp,
						shadowElevation = 2.dp,
						shape = RoundedCornerShape(12.dp),
						color = MaterialTheme.colorScheme.surface,
						modifier =
						(
							if (context.isRunningOnTv()) {
								Modifier
									.fillMaxHeight(fillMaxHeight)
									.fillMaxWidth(fillMaxWidth)
							} else {
								Modifier.fillMaxWidth(fillMaxWidth)
							}
							)
							.padding(top = 10.dp, bottom = 10.dp),
					) {
						Column(
							horizontalAlignment = Alignment.Start,
							verticalArrangement = Arrangement.Top,
							modifier =
							Modifier
								.padding(horizontal = 15.dp)
								.padding(bottom = 10.dp),
						) {
							Row(
								horizontalArrangement = Arrangement.SpaceBetween,
								verticalAlignment = Alignment.CenterVertically,
								modifier =
								Modifier
									.fillMaxWidth()
									.padding(horizontal = 5.dp),
							) {
								SectionTitle(
									stringResource(R.string.peer),
									padding = screenPadding,
								)
								IconButton(onClick = {
									peersState.removeAt(index)
								}) {
									val icon = Icons.Rounded.Delete
									Icon(icon, icon.name)
								}
							}

							ConfigurationTextBox(
								value = peer.publicKey,
								onValueChange = { value ->
									peersState[index] = peersState[index].copy(publicKey = value)
								},
								keyboardActions = keyboardActions,
								label = stringResource(R.string.public_key),
								hint = stringResource(R.string.base64_key),
								modifier = Modifier.fillMaxWidth(),
							)
							ConfigurationTextBox(
								value = peer.preSharedKey,
								onValueChange = { value ->
									peersState[index] = peersState[index].copy(preSharedKey = value)
								},
								keyboardActions = keyboardActions,
								label = stringResource(R.string.preshared_key),
								hint = stringResource(R.string.optional),
								modifier = Modifier.fillMaxWidth(),
							)
							OutlinedTextField(
								modifier = Modifier.fillMaxWidth(),
								value = peer.persistentKeepalive,
								enabled = true,
								onValueChange = { value ->
									peersState[index] = peersState[index].copy(persistentKeepalive = value)
								},
								trailingIcon = {
									Text(
										stringResource(R.string.seconds),
										modifier = Modifier.padding(end = 10.dp),
									)
								},
								label = { Text(stringResource(R.string.persistent_keepalive)) },
								singleLine = true,
								placeholder = {
									Text(stringResource(R.string.optional_no_recommend))
								},
								keyboardOptions = keyboardOptions,
								keyboardActions = keyboardActions,
							)
							ConfigurationTextBox(
								value = peer.endpoint,
								onValueChange = { value ->
									peersState[index] = peersState[index].copy(endpoint = value)
								},
								keyboardActions = keyboardActions,
								label = stringResource(R.string.endpoint),
								hint = stringResource(R.string.endpoint).lowercase(),
								modifier = Modifier.fillMaxWidth(),
							)
							OutlinedTextField(
								modifier = Modifier.fillMaxWidth(),
								value = peer.allowedIps,
								enabled = true,
								onValueChange = { value ->
									peersState[index] = peersState[index].copy(allowedIps = value)
								},
								label = { Text(stringResource(R.string.allowed_ips)) },
								singleLine = true,
								placeholder = {
									Text(stringResource(R.string.comma_separated_list))
								},
								keyboardOptions = keyboardOptions,
								keyboardActions = keyboardActions,
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
						.padding(bottom = 140.dp),
				) {
					Row(
						verticalAlignment = Alignment.CenterVertically,
						horizontalArrangement = Arrangement.Center,
					) {
						TextButton(onClick = {
							peersState.add(PeerProxy())
						}) {
							Text(stringResource(R.string.add_peer))
						}
					}
				}
			}
		}
	}
}
