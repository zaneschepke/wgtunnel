package com.zaneschepke.wireguardautotunnel.ui.screens.config

import android.annotation.SuppressLint
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
import androidx.compose.material.icons.outlined.Edit
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
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.Route
import com.zaneschepke.wireguardautotunnel.ui.common.config.ConfigurationTextBox
import com.zaneschepke.wireguardautotunnel.ui.common.config.ConfigurationToggle
import com.zaneschepke.wireguardautotunnel.ui.common.navigation.LocalNavController
import com.zaneschepke.wireguardautotunnel.ui.common.navigation.TopNavBar
import com.zaneschepke.wireguardautotunnel.ui.common.prompt.AuthorizationPrompt
import com.zaneschepke.wireguardautotunnel.ui.common.snackbar.SnackbarController
import com.zaneschepke.wireguardautotunnel.ui.common.text.SectionTitle
import com.zaneschepke.wireguardautotunnel.ui.screens.config.components.ApplicationSelectionDialog
import com.zaneschepke.wireguardautotunnel.ui.screens.main.ConfigType
import com.zaneschepke.wireguardautotunnel.util.Constants
import com.zaneschepke.wireguardautotunnel.util.extensions.isRunningOnTv
import kotlinx.coroutines.delay

@Composable
fun ConfigScreen(tunnelId: Int, focusRequester: FocusRequester) {
	val viewModel = hiltViewModel<ConfigViewModel, ConfigViewModel.ConfigViewModelFactory> { factory ->
		factory.create(tunnelId)
	}

	val context = LocalContext.current
	val snackbar = SnackbarController.current
	val clipboardManager: ClipboardManager = LocalClipboardManager.current
	val keyboardController = LocalSoftwareKeyboardController.current
	val navController = LocalNavController.current

	var showApplicationsDialog by remember { mutableStateOf(false) }
	var showAuthPrompt by remember { mutableStateOf(false) }
	var isAuthenticated by remember { mutableStateOf(false) }
	var configType by remember { mutableStateOf(ConfigType.WIREGUARD) }

	val uiState by viewModel.uiState.collectAsStateWithLifecycle()
	val saved by viewModel.saved.collectAsStateWithLifecycle(null)

	LaunchedEffect(saved) {
		if (saved == true) {
			navController.navigate(Route.Main)
		}
	}

	LaunchedEffect(Unit) {
		if (!uiState.loading && context.isRunningOnTv()) {
			delay(Constants.FOCUS_REQUEST_DELAY)
			kotlin.runCatching {
				focusRequester.requestFocus()
			}.onFailure {
				delay(Constants.FOCUS_REQUEST_DELAY)
				focusRequester.requestFocus()
			}
		}
	}

	LaunchedEffect(Unit) {
		delay(2_000L)
		viewModel.cleanUpUninstalledApps()
	}

	val keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() })
	val keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)

	val fillMaxHeight = .85f
	val fillMaxWidth = .85f
	val screenPadding = 5.dp

	val applicationButtonText =
		buildAnnotatedString {
			append(stringResource(id = R.string.tunneling_apps))
			append(": ")
			if (uiState.isAllApplicationsEnabled) {
				append(stringResource(id = R.string.all))
			} else {
				append("${uiState.checkedPackageNames.size} ")
				(
					if (uiState.include) {
						append(stringResource(id = R.string.included))
					} else {
						append(
							stringResource(id = R.string.excluded),
						)
					}
					)
			}
		}

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

	if (showApplicationsDialog) {
		ApplicationSelectionDialog(viewModel, uiState) {
			showApplicationsDialog = false
		}
	}

	Scaffold(
		topBar = {
			TopNavBar(stringResource(R.string.edit_tunnel))
		},
		floatingActionButtonPosition = FabPosition.End,
		floatingActionButton = {
			FloatingActionButton(
				onClick = {
					viewModel.onSaveAllChanges()
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
	) {
		Column(Modifier.padding(it)) {
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
						.padding(bottom = 10.dp),
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
							checked = configType == ConfigType.AMNEZIA,
							padding = screenPadding,
							onCheckChanged = { configType = if (it) ConfigType.AMNEZIA else ConfigType.WIREGUARD },
							modifier = Modifier.focusRequester(focusRequester),
						)
						ConfigurationTextBox(
							value = uiState.tunnelName,
							onValueChange = viewModel::onTunnelNameChange,
							keyboardActions = keyboardActions,
							label = stringResource(R.string.name),
							hint = stringResource(R.string.tunnel_name).lowercase(),
							modifier =
							Modifier
								.fillMaxWidth()
								.focusRequester(focusRequester),
						)
						OutlinedTextField(
							modifier =
							Modifier
								.fillMaxWidth()
								.clickable { showAuthPrompt = true },
							value = uiState.interfaceProxy.privateKey,
							visualTransformation =
							if ((tunnelId == Constants.MANUAL_TUNNEL_CONFIG_ID.toInt()) || isAuthenticated) {
								VisualTransformation.None
							} else {
								PasswordVisualTransformation()
							},
							enabled = (tunnelId == Constants.MANUAL_TUNNEL_CONFIG_ID.toInt()) || isAuthenticated,
							onValueChange = { value -> viewModel.onPrivateKeyChange(value) },
							trailingIcon = {
								IconButton(
									modifier = Modifier.focusRequester(FocusRequester.Default),
									onClick = { viewModel.generateKeyPair() },
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
							value = uiState.interfaceProxy.publicKey,
							enabled = false,
							onValueChange = {},
							trailingIcon = {
								IconButton(
									modifier = Modifier.focusRequester(FocusRequester.Default),
									onClick = {
										clipboardManager.setText(
											AnnotatedString(uiState.interfaceProxy.publicKey),
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
							value = uiState.interfaceProxy.addresses,
							onValueChange = viewModel::onAddressesChanged,
							keyboardActions = keyboardActions,
							label = stringResource(R.string.addresses),
							hint = stringResource(R.string.comma_separated_list),
							modifier =
							Modifier
								.fillMaxWidth()
								.padding(end = 5.dp),
						)
						ConfigurationTextBox(
							value = uiState.interfaceProxy.listenPort,
							onValueChange = viewModel::onListenPortChanged,
							keyboardActions = keyboardActions,
							label = stringResource(R.string.listen_port),
							hint = stringResource(R.string.random),
							modifier = Modifier.fillMaxWidth(),
						)
						Row(modifier = Modifier.fillMaxWidth()) {
							ConfigurationTextBox(
								value = uiState.interfaceProxy.dnsServers,
								onValueChange = viewModel::onDnsServersChanged,
								keyboardActions = keyboardActions,
								label = stringResource(R.string.dns_servers),
								hint = stringResource(R.string.comma_separated_list),
								modifier =
								Modifier
									.fillMaxWidth(3 / 5f)
									.padding(end = 5.dp),
							)
							ConfigurationTextBox(
								value = uiState.interfaceProxy.mtu,
								onValueChange = viewModel::onMtuChanged,
								keyboardActions = keyboardActions,
								label = stringResource(R.string.mtu),
								hint = stringResource(R.string.auto),
								modifier = Modifier.width(IntrinsicSize.Min),
							)
						}
						if (configType == ConfigType.AMNEZIA) {
							ConfigurationTextBox(
								value = uiState.interfaceProxy.junkPacketCount,
								onValueChange = viewModel::onJunkPacketCountChanged,
								keyboardActions = keyboardActions,
								label = stringResource(R.string.junk_packet_count),
								hint = stringResource(R.string.junk_packet_count).lowercase(),
								modifier =
								Modifier
									.fillMaxWidth()
									.focusRequester(focusRequester),
							)
							ConfigurationTextBox(
								value = uiState.interfaceProxy.junkPacketMinSize,
								onValueChange = viewModel::onJunkPacketMinSizeChanged,
								keyboardActions = keyboardActions,
								label = stringResource(R.string.junk_packet_minimum_size),
								hint =
								stringResource(
									R.string.junk_packet_minimum_size,
								).lowercase(),
								modifier =
								Modifier
									.fillMaxWidth()
									.focusRequester(focusRequester),
							)
							ConfigurationTextBox(
								value = uiState.interfaceProxy.junkPacketMaxSize,
								onValueChange = viewModel::onJunkPacketMaxSizeChanged,
								keyboardActions = keyboardActions,
								label = stringResource(R.string.junk_packet_maximum_size),
								hint =
								stringResource(
									R.string.junk_packet_maximum_size,
								).lowercase(),
								modifier =
								Modifier
									.fillMaxWidth()
									.focusRequester(focusRequester),
							)
							ConfigurationTextBox(
								value = uiState.interfaceProxy.initPacketJunkSize,
								onValueChange = viewModel::onInitPacketJunkSizeChanged,
								keyboardActions = keyboardActions,
								label = stringResource(R.string.init_packet_junk_size),
								hint = stringResource(R.string.init_packet_junk_size).lowercase(),
								modifier =
								Modifier
									.fillMaxWidth()
									.focusRequester(focusRequester),
							)
							ConfigurationTextBox(
								value = uiState.interfaceProxy.responsePacketJunkSize,
								onValueChange = viewModel::onResponsePacketJunkSize,
								keyboardActions = keyboardActions,
								label = stringResource(R.string.response_packet_junk_size),
								hint =
								stringResource(
									R.string.response_packet_junk_size,
								).lowercase(),
								modifier =
								Modifier
									.fillMaxWidth()
									.focusRequester(focusRequester),
							)
							ConfigurationTextBox(
								value = uiState.interfaceProxy.initPacketMagicHeader,
								onValueChange = viewModel::onInitPacketMagicHeader,
								keyboardActions = keyboardActions,
								label = stringResource(R.string.init_packet_magic_header),
								hint =
								stringResource(
									R.string.init_packet_magic_header,
								).lowercase(),
								modifier =
								Modifier
									.fillMaxWidth()
									.focusRequester(focusRequester),
							)
							ConfigurationTextBox(
								value = uiState.interfaceProxy.responsePacketMagicHeader,
								onValueChange = viewModel::onResponsePacketMagicHeader,
								keyboardActions = keyboardActions,
								label = stringResource(R.string.response_packet_magic_header),
								hint =
								stringResource(
									R.string.response_packet_magic_header,
								).lowercase(),
								modifier =
								Modifier
									.fillMaxWidth()
									.focusRequester(focusRequester),
							)
							ConfigurationTextBox(
								value = uiState.interfaceProxy.underloadPacketMagicHeader,
								onValueChange = viewModel::onUnderloadPacketMagicHeader,
								keyboardActions = keyboardActions,
								label = stringResource(R.string.underload_packet_magic_header),
								hint =
								stringResource(
									R.string.underload_packet_magic_header,
								).lowercase(),
								modifier =
								Modifier
									.fillMaxWidth()
									.focusRequester(focusRequester),
							)
							ConfigurationTextBox(
								value = uiState.interfaceProxy.transportPacketMagicHeader,
								onValueChange = viewModel::onTransportPacketMagicHeader,
								keyboardActions = keyboardActions,
								label = stringResource(R.string.transport_packet_magic_header),
								hint =
								stringResource(
									R.string.transport_packet_magic_header,
								).lowercase(),
								modifier =
								Modifier
									.fillMaxWidth()
									.focusRequester(focusRequester),
							)
						}
						Row(
							verticalAlignment = Alignment.CenterVertically,
							modifier =
							Modifier
								.fillMaxSize()
								.padding(top = 5.dp),
							horizontalArrangement = Arrangement.Center,
						) {
							TextButton(onClick = { showApplicationsDialog = true }) {
								Text(applicationButtonText.text)
							}
						}
					}
				}
				uiState.proxyPeers.forEachIndexed { index, peer ->
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
								IconButton(onClick = { viewModel.onDeletePeer(index) }) {
									val icon = Icons.Rounded.Delete
									Icon(icon, icon.name)
								}
							}

							ConfigurationTextBox(
								value = peer.publicKey,
								onValueChange = { value ->
									viewModel.onPeerPublicKeyChange(index, value)
								},
								keyboardActions = keyboardActions,
								label = stringResource(R.string.public_key),
								hint = stringResource(R.string.base64_key),
								modifier = Modifier.fillMaxWidth(),
							)
							ConfigurationTextBox(
								value = peer.preSharedKey,
								onValueChange = { value ->
									viewModel.onPreSharedKeyChange(index, value)
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
									viewModel.onPersistentKeepaliveChanged(index, value)
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
									viewModel.onEndpointChange(index, value)
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
									viewModel.onAllowedIpsChange(index, value)
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
						TextButton(onClick = { viewModel.addEmptyPeer() }) {
							Text(stringResource(R.string.add_peer))
						}
					}
				}
			}
		}
	}
}
