package com.zaneschepke.wireguardautotunnel.ui.screens.tunneloptions.tunnelautotunnel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NetworkPing
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.SettingsEthernet
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.data.domain.Settings
import com.zaneschepke.wireguardautotunnel.data.domain.TunnelConfig
import com.zaneschepke.wireguardautotunnel.ui.common.button.ScaledSwitch
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItem
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SurfaceSelectionGroupButton
import com.zaneschepke.wireguardautotunnel.ui.common.config.SubmitConfigurationTextBox
import com.zaneschepke.wireguardautotunnel.ui.common.navigation.TopNavBar
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.autotunnel.components.TrustedNetworkTextBox
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.autotunnel.components.WildcardsLabel
import com.zaneschepke.wireguardautotunnel.ui.theme.iconSize
import com.zaneschepke.wireguardautotunnel.util.Constants
import com.zaneschepke.wireguardautotunnel.util.extensions.isValidIpv4orIpv6Address
import com.zaneschepke.wireguardautotunnel.util.extensions.scaledHeight
import com.zaneschepke.wireguardautotunnel.util.extensions.scaledWidth

@Composable
fun TunnelAutoTunnelScreen(tunnelConfig: TunnelConfig, settings: Settings, tunnelAutoTunnelViewModel: TunnelAutoTunnelViewModel = hiltViewModel()) {
	var currentText by remember { mutableStateOf("") }

	LaunchedEffect(tunnelConfig.tunnelNetworks) {
		currentText = ""
	}
	Scaffold(
		topBar = {
			TopNavBar(tunnelConfig.name)
		},
	) { padding ->
		Column(
			horizontalAlignment = Alignment.Start,
			verticalArrangement = Arrangement.spacedBy(24.dp.scaledHeight(), Alignment.Top),
			modifier =
			Modifier
				.fillMaxSize()
				.padding(padding)
				.verticalScroll(rememberScrollState())
				.padding(top = 24.dp.scaledHeight())
				.padding(horizontal = 24.dp.scaledWidth()),
		) {
			SurfaceSelectionGroupButton(
				buildList {
					addAll(
						listOf(
							SelectionItem(
								Icons.Outlined.PhoneAndroid,
								title = {
									Text(
										stringResource(R.string.mobile_tunnel),
										style = MaterialTheme.typography.bodyMedium.copy(MaterialTheme.colorScheme.onSurface),
									)
								},
								description = {
									Text(
										stringResource(R.string.mobile_data_tunnel),
										style = MaterialTheme.typography.bodySmall.copy(MaterialTheme.colorScheme.outline),
									)
								},
								trailing = {
									ScaledSwitch(
										tunnelConfig.isMobileDataTunnel,
										onClick = { tunnelAutoTunnelViewModel.onToggleIsMobileDataTunnel(tunnelConfig) },
									)
								},
								onClick = { tunnelAutoTunnelViewModel.onToggleIsMobileDataTunnel(tunnelConfig) },
							),
							SelectionItem(
								Icons.Outlined.SettingsEthernet,
								title = {
									Text(
										stringResource(R.string.ethernet_tunnel),
										style = MaterialTheme.typography.bodyMedium.copy(MaterialTheme.colorScheme.onSurface),
									)
								},
								description = {
									Text(
										stringResource(R.string.set_ethernet_tunnel),
										style = MaterialTheme.typography.bodySmall.copy(MaterialTheme.colorScheme.outline),
									)
								},
								trailing = {
									ScaledSwitch(
										tunnelConfig.isEthernetTunnel,
										onClick = { tunnelAutoTunnelViewModel.onToggleIsEthernetTunnel(tunnelConfig) },
									)
								},
								onClick = { tunnelAutoTunnelViewModel.onToggleIsEthernetTunnel(tunnelConfig) },
							),
							SelectionItem(
								Icons.Outlined.NetworkPing,
								title = {
									Text(
										stringResource(R.string.restart_on_ping),
										style = MaterialTheme.typography.bodyMedium.copy(MaterialTheme.colorScheme.onSurface),
									)
								},
								trailing = {
									ScaledSwitch(
										checked = tunnelConfig.isPingEnabled,
										onClick = { tunnelAutoTunnelViewModel.onToggleRestartOnPing(tunnelConfig) },
									)
								},
								onClick = { tunnelAutoTunnelViewModel.onToggleRestartOnPing(tunnelConfig) },
							),
						),
					)
					if (tunnelConfig.isPingEnabled || settings.isPingEnabled) {
						add(
							SelectionItem(
								title = {},
								description = {
									SubmitConfigurationTextBox(
										tunnelConfig.pingIp,
										stringResource(R.string.set_custom_ping_ip),
										stringResource(R.string.default_ping_ip),
										isErrorValue = { !it.isNullOrBlank() && !it.isValidIpv4orIpv6Address() },
										onSubmit = {
											tunnelAutoTunnelViewModel.saveTunnelChanges(
												tunnelConfig.copy(pingIp = it.ifBlank { null }),
											)
										},
									)
									fun isSecondsError(seconds: String?): Boolean {
										return seconds?.let { value -> if (value.isBlank()) false else value.toLong() >= Long.MAX_VALUE / 1000 } ?: false
									}
									SubmitConfigurationTextBox(
										tunnelConfig.pingInterval?.let { (it / 1000).toString() },
										stringResource(R.string.set_custom_ping_internal),
										"(${stringResource(R.string.optional_default)} ${Constants.PING_INTERVAL / 1000})",
										keyboardOptions = KeyboardOptions(
											keyboardType = KeyboardType.Number,
											imeAction = ImeAction.Done,
										),
										isErrorValue = ::isSecondsError,
										onSubmit = {
											tunnelAutoTunnelViewModel.saveTunnelChanges(
												tunnelConfig.copy(pingInterval = if (it.isBlank()) null else it.toLong() * 1000),
											)
										},
									)
									SubmitConfigurationTextBox(
										tunnelConfig.pingCooldown?.let { (it / 1000).toString() },
										stringResource(R.string.set_custom_ping_cooldown),
										"(${stringResource(R.string.optional_default)} ${Constants.PING_COOLDOWN / 1000})",
										keyboardOptions = KeyboardOptions(
											keyboardType = KeyboardType.Number,
										),
										isErrorValue = ::isSecondsError,
										onSubmit = {
											tunnelAutoTunnelViewModel.saveTunnelChanges(
												tunnelConfig.copy(pingCooldown = if (it.isBlank()) null else it.toLong() * 1000),
											)
										},
									)
								},
							),
						)
					}
					add(
						SelectionItem(
							title = {
								Row(
									verticalAlignment = Alignment.CenterVertically,
									modifier = Modifier
										.fillMaxWidth()
										.padding(vertical = 4.dp.scaledHeight()),
								) {
									Row(
										verticalAlignment = Alignment.CenterVertically,
										modifier = Modifier
											.weight(4f, false)
											.fillMaxWidth(),
									) {
										val icon = Icons.Outlined.Security
										Icon(
											icon,
											icon.name,
											modifier = Modifier.size(iconSize),
										)
										Column(
											horizontalAlignment = Alignment.Start,
											verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
											modifier = Modifier
												.fillMaxWidth()
												.padding(start = 16.dp.scaledWidth())
												.padding(vertical = 6.dp.scaledHeight()),
										) {
											Text(
												stringResource(R.string.use_tunnel_on_wifi_name),
												style = MaterialTheme.typography.bodyMedium.copy(MaterialTheme.colorScheme.onSurface),
											)
										}
									}
								}
							},
							description = {
								TrustedNetworkTextBox(
									tunnelConfig.tunnelNetworks,
									onDelete = { tunnelAutoTunnelViewModel.onDeleteRunSSID(it, tunnelConfig) },
									currentText = currentText,
									onSave = { tunnelAutoTunnelViewModel.onSaveRunSSID(it, tunnelConfig) },
									onValueChange = { currentText = it },
									supporting = {
										if (settings.isWildcardsEnabled) {
											WildcardsLabel()
										}
									},
								)
							},
						),
					)
				},
			)
		}
	}
}
