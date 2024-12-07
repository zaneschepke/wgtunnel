package com.zaneschepke.wireguardautotunnel.ui.screens.settings.killswitch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Lan
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.AppUiState
import com.zaneschepke.wireguardautotunnel.ui.AppViewModel
import com.zaneschepke.wireguardautotunnel.ui.common.button.ScaledSwitch
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItem
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SurfaceSelectionGroupButton
import com.zaneschepke.wireguardautotunnel.ui.common.navigation.TopNavBar
import com.zaneschepke.wireguardautotunnel.ui.common.permission.vpn.withVpnPermission
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.components.ForwardButton
import com.zaneschepke.wireguardautotunnel.util.extensions.launchVpnSettings
import com.zaneschepke.wireguardautotunnel.util.extensions.scaledHeight
import com.zaneschepke.wireguardautotunnel.util.extensions.scaledWidth

@Composable
fun KillSwitchScreen(uiState: AppUiState, appViewModel: AppViewModel) {
	val context = LocalContext.current

	val toggleVpnSwitch = withVpnPermission<Boolean> { appViewModel.onToggleVpnKillSwitch(it) }

	fun toggleVpnKillSwitch() {
		with(uiState.settings) {
			if (isVpnKillSwitchEnabled) {
				appViewModel.onToggleVpnKillSwitch(false)
			} else {
				toggleVpnSwitch.invoke(true)
			}
		}
	}

	fun toggleLanOnKillSwitch() {
		with(uiState.settings) {
			appViewModel.onToggleLanOnKillSwitch(!isLanOnKillSwitchEnabled)
		}
	}

	Scaffold(
		topBar = {
			TopNavBar(stringResource(R.string.kill_switch))
		},
	) { padding ->
		Column(
			horizontalAlignment = Alignment.Start,
			verticalArrangement = Arrangement.spacedBy(24.dp.scaledHeight(), Alignment.Top),
			modifier =
			Modifier
				.fillMaxSize().padding(padding)
				.padding(top = 24.dp.scaledHeight())
				.padding(horizontal = 24.dp.scaledWidth()),
		) {
			SurfaceSelectionGroupButton(
				listOf(
					SelectionItem(
						Icons.Outlined.AdminPanelSettings,
						title = {
							Text(
								stringResource(R.string.native_kill_switch),
								style = MaterialTheme.typography.bodyMedium.copy(MaterialTheme.colorScheme.onSurface),
							)
						},
						onClick = { context.launchVpnSettings() },
						trailing = {
							ForwardButton { context.launchVpnSettings() }
						},
					),
				),
			)
			SurfaceSelectionGroupButton(
				buildList {
					add(
						SelectionItem(
							Icons.Outlined.VpnKey,
							title = {
								Text(
									stringResource(R.string.vpn_kill_switch),
									style = MaterialTheme.typography.bodyMedium.copy(MaterialTheme.colorScheme.onSurface),
								)
							},
							onClick = {
								toggleVpnKillSwitch()
							},
							trailing = {
								ScaledSwitch(
									uiState.settings.isVpnKillSwitchEnabled,
									onClick = {
										toggleVpnKillSwitch()
									},
								)
							},
						),
					)
					if (uiState.settings.isVpnKillSwitchEnabled) {
						add(
							SelectionItem(
								Icons.Outlined.Lan,
								title = {
									Text(
										stringResource(R.string.allow_lan_traffic),
										style = MaterialTheme.typography.bodyMedium.copy(MaterialTheme.colorScheme.onSurface),
									)
								},
								onClick = { toggleLanOnKillSwitch() },
								description = {
									Text(
										stringResource(R.string.bypass_lan_for_kill_switch),
										style = MaterialTheme.typography.bodySmall.copy(MaterialTheme.colorScheme.outline),
									)
								},
								trailing = {
									ScaledSwitch(
										uiState.settings.isLanOnKillSwitchEnabled,
										onClick = {
											toggleLanOnKillSwitch()
										},
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
