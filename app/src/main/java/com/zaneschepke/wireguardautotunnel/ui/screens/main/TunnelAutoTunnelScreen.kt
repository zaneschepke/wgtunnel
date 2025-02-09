package com.zaneschepke.wireguardautotunnel.ui.screens.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.domain.entity.AppSettings
import com.zaneschepke.wireguardautotunnel.domain.entity.TunnelConf
import com.zaneschepke.wireguardautotunnel.ui.common.button.ScaledSwitch
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItem
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SurfaceSelectionGroupButton
import com.zaneschepke.wireguardautotunnel.ui.common.navigation.TopNavBar
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.autotunnel.components.TrustedNetworkTextBox
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.autotunnel.components.WildcardsLabel
import com.zaneschepke.wireguardautotunnel.ui.theme.iconSize
import com.zaneschepke.wireguardautotunnel.util.extensions.scaledHeight
import com.zaneschepke.wireguardautotunnel.util.extensions.scaledWidth
import com.zaneschepke.wireguardautotunnel.viewmodel.TunnelAutoTunnelViewModel

@Composable
fun TunnelAutoTunnelScreen(tunnelConf: TunnelConf, appSettings: AppSettings, tunnelAutoTunnelViewModel: TunnelAutoTunnelViewModel = hiltViewModel()) {
	var currentText by remember { mutableStateOf("") }

	LaunchedEffect(tunnelConf.tunnelNetworks) {
		currentText = ""
	}
	Scaffold(
		topBar = {
			TopNavBar(tunnelConf.tunName)
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
										tunnelConf.isMobileDataTunnel,
										onClick = { tunnelAutoTunnelViewModel.onToggleIsMobileDataTunnel(tunnelConf) },
									)
								},
								onClick = { tunnelAutoTunnelViewModel.onToggleIsMobileDataTunnel(tunnelConf) },
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
										tunnelConf.isEthernetTunnel,
										onClick = { tunnelAutoTunnelViewModel.onToggleIsEthernetTunnel(tunnelConf) },
									)
								},
								onClick = { tunnelAutoTunnelViewModel.onToggleIsEthernetTunnel(tunnelConf) },
							),
						),
					)
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
									tunnelConf.tunnelNetworks,
									onDelete = { tunnelAutoTunnelViewModel.onDeleteRunSSID(it, tunnelConf) },
									currentText = currentText,
									onSave = { tunnelAutoTunnelViewModel.onSaveRunSSID(it, tunnelConf) },
									onValueChange = { currentText = it },
									supporting = {
										if (appSettings.isWildcardsEnabled) {
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
