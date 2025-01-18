package com.zaneschepke.wireguardautotunnel.ui.screens.tunneloptions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.CallSplit
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.NetworkPing
import androidx.compose.material.icons.outlined.Star
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
import com.zaneschepke.wireguardautotunnel.data.domain.TunnelConfig
import com.zaneschepke.wireguardautotunnel.ui.Route
import com.zaneschepke.wireguardautotunnel.ui.common.button.ScaledSwitch
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItem
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SurfaceSelectionGroupButton
import com.zaneschepke.wireguardautotunnel.ui.common.config.SubmitConfigurationTextBox
import com.zaneschepke.wireguardautotunnel.ui.common.navigation.LocalNavController
import com.zaneschepke.wireguardautotunnel.ui.common.navigation.TopNavBar
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.components.ForwardButton
import com.zaneschepke.wireguardautotunnel.util.Constants
import com.zaneschepke.wireguardautotunnel.util.extensions.isValidIpv4orIpv6Address
import com.zaneschepke.wireguardautotunnel.util.extensions.scaledHeight
import com.zaneschepke.wireguardautotunnel.util.extensions.scaledWidth
import kotlin.text.isBlank
import kotlin.text.isNullOrBlank
import kotlin.text.toLong

@Composable
fun OptionsScreen(tunnelConfig: TunnelConfig, viewModel: TunnelOptionsViewModel = hiltViewModel()) {
	val navController = LocalNavController.current

	var currentText by remember { mutableStateOf("") }

	LaunchedEffect(tunnelConfig.tunnelNetworks) {
		currentText = ""
	}

	val onPingToggle = {
		viewModel.saveTunnelChanges(tunnelConfig.copy(isPingEnabled = !tunnelConfig.isPingEnabled))
	}

	Scaffold(
		topBar = {
			TopNavBar(tunnelConfig.name)
		},
	) {
		Column(
			horizontalAlignment = Alignment.Start,
			verticalArrangement = Arrangement.spacedBy(24.dp.scaledHeight(), Alignment.Top),
			modifier =
			Modifier
				.fillMaxSize()
				.padding(it)
				.imePadding()
				.verticalScroll(rememberScrollState())
				.padding(top = 24.dp.scaledHeight())
				.padding(horizontal = 24.dp.scaledWidth()),
		) {
			SurfaceSelectionGroupButton(
				listOf(
					SelectionItem(
						Icons.Outlined.Star,
						title = {
							Text(
								stringResource(R.string.primary_tunnel),
								style = MaterialTheme.typography.bodyMedium.copy(MaterialTheme.colorScheme.onSurface),
							)
						},
						description = {
							Text(
								stringResource(R.string.set_primary_tunnel),
								style = MaterialTheme.typography.bodySmall.copy(MaterialTheme.colorScheme.outline),
							)
						},
						trailing = {
							ScaledSwitch(
								tunnelConfig.isPrimaryTunnel,
								onClick = { viewModel.onTogglePrimaryTunnel(tunnelConfig) },
							)
						},
						onClick = { viewModel.onTogglePrimaryTunnel(tunnelConfig) },
					),
					SelectionItem(
						Icons.Outlined.Bolt,
						title = {
							Text(
								stringResource(R.string.auto_tunneling),
								style = MaterialTheme.typography.bodyMedium.copy(MaterialTheme.colorScheme.onSurface),
							)
						},
						description = {
							Text(
								stringResource(R.string.tunnel_specific_settings),
								style = MaterialTheme.typography.bodySmall.copy(MaterialTheme.colorScheme.outline),
							)
						},
						onClick = {
							navController.navigate(Route.TunnelAutoTunnel(id = tunnelConfig.id))
						},
						trailing = {
							ForwardButton { navController.navigate(Route.TunnelAutoTunnel(id = tunnelConfig.id)) }
						},
					),
					SelectionItem(
						Icons.Outlined.Edit,
						title = {
							Text(
								stringResource(R.string.edit_tunnel),
								style = MaterialTheme.typography.bodyMedium.copy(MaterialTheme.colorScheme.onSurface),
							)
						},
						onClick = {
							navController.navigate(Route.Config(id = tunnelConfig.id))
						},
						trailing = {
							ForwardButton { navController.navigate(Route.Config(id = tunnelConfig.id)) }
						},
					),
					SelectionItem(
						Icons.AutoMirrored.Outlined.CallSplit,
						title = {
							Text(
								stringResource(R.string.splt_tunneling),
								style = MaterialTheme.typography.bodyMedium.copy(MaterialTheme.colorScheme.onSurface),
							)
						},
						onClick = {
							navController.navigate(Route.SplitTunnel(id = tunnelConfig.id))
						},
						trailing = {
							ForwardButton { navController.navigate(Route.SplitTunnel(id = tunnelConfig.id)) }
						},
					),
				),
			)
			SurfaceSelectionGroupButton(
				buildList {
					add(
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
									onClick = { onPingToggle() },
								)
							},
							onClick = { onPingToggle() },
						),
					)
					if (tunnelConfig.isPingEnabled) {
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
											viewModel.saveTunnelChanges(
												tunnelConfig.copy(pingIp = it.ifBlank { null }),
											)
										},
									)
									fun isSecondsError(seconds: String?): Boolean {
										return seconds?.let { value -> if (value.isBlank()) false else value.toLong() >= Long.MAX_VALUE / 1000 } == true
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
											viewModel.onPingIntervalChange(tunnelConfig, it)
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
										onSubmit = { viewModel.onPingCoolDownChange(tunnelConfig, it) },
									)
								},
							),
						)
					}
				},
			)
		}
	}
}
