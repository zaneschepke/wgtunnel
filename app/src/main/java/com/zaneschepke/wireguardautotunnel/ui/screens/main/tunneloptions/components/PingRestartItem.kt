package com.zaneschepke.wireguardautotunnel.ui.screens.main.tunneloptions.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NetworkPing
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.core.tunnel.isUp
import com.zaneschepke.wireguardautotunnel.domain.entity.TunnelConf
import com.zaneschepke.wireguardautotunnel.ui.common.button.ScaledSwitch
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItem
import com.zaneschepke.wireguardautotunnel.ui.state.AppUiState
import com.zaneschepke.wireguardautotunnel.viewmodel.AppViewModel
import com.zaneschepke.wireguardautotunnel.viewmodel.event.AppEvent

@Composable
fun PingRestartItem(tunnelConf: TunnelConf, appUiState: AppUiState, viewModel: AppViewModel): SelectionItem {
	return SelectionItem(
		leadingIcon = Icons.Outlined.NetworkPing,
		title = {
			Text(
				text = stringResource(R.string.restart_on_ping),
				style = MaterialTheme.typography.bodyMedium.copy(MaterialTheme.colorScheme.onSurface),
			)
		},
		trailing = {
			ScaledSwitch(
				checked = tunnelConf.isPingEnabled,
				enabled = !appUiState.activeTunnels.isUp(tunnelConf),
				onClick = { viewModel.handleEvent(AppEvent.TogglePingTunnelEnabled(tunnelConf)) },
			)
		},
		onClick = { viewModel.handleEvent(AppEvent.TogglePingTunnelEnabled(tunnelConf)) },
	)
}
