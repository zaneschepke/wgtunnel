package com.zaneschepke.wireguardautotunnel.ui.screens.options

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.AppUiState
import com.zaneschepke.wireguardautotunnel.ui.Route
import com.zaneschepke.wireguardautotunnel.ui.common.ClickableIconButton
import com.zaneschepke.wireguardautotunnel.ui.common.config.ConfigurationToggle
import com.zaneschepke.wireguardautotunnel.ui.common.config.SubmitConfigurationTextBox
import com.zaneschepke.wireguardautotunnel.ui.common.navigation.LocalNavController
import com.zaneschepke.wireguardautotunnel.ui.common.navigation.TopNavBar
import com.zaneschepke.wireguardautotunnel.ui.common.text.SectionTitle
import com.zaneschepke.wireguardautotunnel.ui.screens.main.components.ScrollDismissFab
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.components.WildcardSupportingLabel
import com.zaneschepke.wireguardautotunnel.util.Constants
import com.zaneschepke.wireguardautotunnel.util.extensions.isRunningOnTv
import com.zaneschepke.wireguardautotunnel.util.extensions.isValidIpv4orIpv6Address
import com.zaneschepke.wireguardautotunnel.util.extensions.openWebUrl
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OptionsScreen(optionsViewModel: OptionsViewModel = hiltViewModel(), focusRequester: FocusRequester, appUiState: AppUiState, tunnelId: Int) {
	val scrollState = rememberScrollState()
	val context = LocalContext.current
	val navController = LocalNavController.current
	val config = appUiState.tunnels.first { it.id == tunnelId }

	val interactionSource = remember { MutableInteractionSource() }
	val focusManager = LocalFocusManager.current
	val screenPadding = 5.dp
	val fillMaxWidth = .85f

	var currentText by remember { mutableStateOf("") }

	LaunchedEffect(Unit) {
		if (context.isRunningOnTv()) {
			delay(Constants.FOCUS_REQUEST_DELAY)
			kotlin.runCatching {
				focusRequester.requestFocus()
			}.onFailure {
				delay(Constants.FOCUS_REQUEST_DELAY)
				focusRequester.requestFocus()
			}
		}
	}

	fun saveTrustedSSID() {
		if (currentText.isNotEmpty()) {
			optionsViewModel.onSaveRunSSID(currentText, config)
			currentText = ""
		}
	}

	Scaffold(
		topBar = {
			TopNavBar(config.name, trailing = {
				IconButton(onClick = { navController.navigate(
					Route.Config(config.id),
				) }) {
					val icon = Icons.Outlined.Edit
					Icon(
						imageVector = icon,
						contentDescription = icon.name
					)
				}
			})
		},
	) {
		Column(
			horizontalAlignment = Alignment.CenterHorizontally,
			verticalArrangement = Arrangement.Top,
			modifier =
			Modifier
				.fillMaxSize().padding(it)
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
				(
					if (context.isRunningOnTv()) {
						Modifier
							.height(IntrinsicSize.Min)
							.fillMaxWidth(fillMaxWidth)
							.padding(top = 10.dp)
					} else {
						Modifier
							.fillMaxWidth(fillMaxWidth)
							.padding(top = 20.dp)
					}
					)
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
						checked = config.isPrimaryTunnel,
						modifier =
						Modifier
							.focusRequester(focusRequester),
						padding = screenPadding,
						onCheckChanged = { optionsViewModel.onTogglePrimaryTunnel(config) },
					)
				}
			}
			Surface(
				tonalElevation = 2.dp,
				shadowElevation = 2.dp,
				shape = RoundedCornerShape(12.dp),
				color = MaterialTheme.colorScheme.surface,
				modifier =
				(
					if (context.isRunningOnTv()) {
						Modifier
							.height(IntrinsicSize.Min)
							.fillMaxWidth(fillMaxWidth)
							.padding(top = 10.dp)
					} else {
						Modifier
							.fillMaxWidth(fillMaxWidth)
							.padding(top = 20.dp)
					}
					)
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
						checked = config.isMobileDataTunnel,
						padding = screenPadding,
						onCheckChanged = { optionsViewModel.onToggleIsMobileDataTunnel(config) },
					)
					Column {
						FlowRow(
							modifier =
							Modifier
								.padding(screenPadding)
								.fillMaxWidth(),
							horizontalArrangement = Arrangement.spacedBy(5.dp),
						) {
							config.tunnelNetworks.forEach { ssid ->
								ClickableIconButton(
									onClick = {
										if (context.isRunningOnTv()) {
											focusRequester.requestFocus()
											optionsViewModel.onDeleteRunSSID(ssid, config)
										}
									},
									onIconClick = {
										if (context.isRunningOnTv()) focusRequester.requestFocus()
										optionsViewModel.onDeleteRunSSID(ssid, config)
									},
									text = ssid,
									icon = Icons.Filled.Close,
									enabled = true,
								)
							}
							if (config.tunnelNetworks.isEmpty()) {
								Text(
									stringResource(R.string.no_wifi_names_configured),
									fontStyle = FontStyle.Italic,
									color = MaterialTheme.colorScheme.onSurface,
								)
							}
						}
						OutlinedTextField(
							enabled = true,
							value = currentText,
							onValueChange = { currentText = it },
							label = { Text(stringResource(id = R.string.use_tunnel_on_wifi_name)) },
							supportingText = { WildcardSupportingLabel { context.openWebUrl(it) } },
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
											contentDescription = stringResource(R.string.save_changes),
											tint = MaterialTheme.colorScheme.primary,
										)
									}
								}
							},
						)
						ConfigurationToggle(
							stringResource(R.string.restart_on_ping),
							enabled = !appUiState.settings.isPingEnabled,
							checked = config.isPingEnabled || appUiState.settings.isPingEnabled,
							padding = screenPadding,
							onCheckChanged = { optionsViewModel.onToggleRestartOnPing(config) },
						)
						if (config.isPingEnabled || appUiState.settings.isPingEnabled) {
							SubmitConfigurationTextBox(
								config.pingIp,
								stringResource(R.string.set_custom_ping_ip),
								stringResource(R.string.default_ping_ip),
								focusRequester,
								isErrorValue = { !it.isNullOrBlank() && !it.isValidIpv4orIpv6Address() },
								onSubmit = {
									optionsViewModel.saveTunnelChanges(
										config.copy(pingIp = it.ifBlank { null }),
									)
								},
							)
							fun isSecondsError(seconds: String?): Boolean {
								return seconds?.let { value -> if (value.isBlank()) false else value.toLong() >= Long.MAX_VALUE / 1000 } ?: false
							}
							SubmitConfigurationTextBox(
								config.pingInterval?.let { (it / 1000).toString() },
								stringResource(R.string.set_custom_ping_internal),
								"(${stringResource(R.string.optional_default)} ${Constants.PING_INTERVAL / 1000})",
								focusRequester,
								keyboardOptions = KeyboardOptions(
									keyboardType = KeyboardType.Number,
									imeAction = ImeAction.Done,
								),
								isErrorValue = ::isSecondsError,
								onSubmit = {
									optionsViewModel.saveTunnelChanges(
										config.copy(pingInterval = if (it.isBlank()) null else it.toLong() * 1000),
									)
								},
							)
							SubmitConfigurationTextBox(
								config.pingCooldown?.let { (it / 1000).toString() },
								stringResource(R.string.set_custom_ping_cooldown),
								"(${stringResource(R.string.optional_default)} ${Constants.PING_COOLDOWN / 1000})",
								focusRequester,
								keyboardOptions = KeyboardOptions(
									keyboardType = KeyboardType.Number,
								),
								isErrorValue = ::isSecondsError,
								onSubmit = {
									optionsViewModel.saveTunnelChanges(
										config.copy(pingCooldown = if (it.isBlank()) null else it.toLong() * 1000),
									)
								},
							)
						}
					}
				}
			}
		}
	}
}
