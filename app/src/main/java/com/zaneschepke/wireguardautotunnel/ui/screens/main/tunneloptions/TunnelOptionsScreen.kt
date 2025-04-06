package com.zaneschepke.wireguardautotunnel.ui.screens.main.tunneloptions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.domain.entity.TunnelConf
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SurfaceSelectionGroupButton
import com.zaneschepke.wireguardautotunnel.ui.screens.main.tunneloptions.components.AutoTunnelingItem
import com.zaneschepke.wireguardautotunnel.ui.screens.main.tunneloptions.components.PingConfigItem
import com.zaneschepke.wireguardautotunnel.ui.screens.main.tunneloptions.components.PingRestartItem
import com.zaneschepke.wireguardautotunnel.ui.screens.main.tunneloptions.components.PrimaryTunnelItem
import com.zaneschepke.wireguardautotunnel.ui.screens.main.tunneloptions.components.ServerIpv4Item
import com.zaneschepke.wireguardautotunnel.ui.screens.main.tunneloptions.components.SplitTunnelingItem
import com.zaneschepke.wireguardautotunnel.ui.state.AppUiState

import com.zaneschepke.wireguardautotunnel.viewmodel.AppViewModel

@Composable
fun TunnelOptionsScreen(tunnelConf: TunnelConf, appUiState: AppUiState, viewModel: AppViewModel) {
	var currentText by remember { mutableStateOf("") }

	LaunchedEffect(tunnelConf.tunnelNetworks) {
		currentText = ""
	}

	Column(
		horizontalAlignment = Alignment.Start,
		verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.Top),
		modifier = Modifier
			.fillMaxSize()
			.verticalScroll(rememberScrollState())
			.padding(top = 24.dp)
			.padding(horizontal = 24.dp),
	) {
		SurfaceSelectionGroupButton(
			items = listOf(
				PrimaryTunnelItem(tunnelConf, viewModel),
				AutoTunnelingItem(tunnelConf),
				ServerIpv4Item(tunnelConf, viewModel),
				SplitTunnelingItem(tunnelConf),
			),
		)
		SurfaceSelectionGroupButton(
			items = buildList {
				add(PingRestartItem(tunnelConf, viewModel))
				if (tunnelConf.isPingEnabled) {
					add(PingConfigItem(tunnelConf, viewModel))
				}
			},
		)
	}
}
