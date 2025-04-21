package com.zaneschepke.wireguardautotunnel.ui.screens.main.tunneloptions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.domain.entity.TunnelConf
import com.zaneschepke.wireguardautotunnel.ui.common.SectionDivider
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SurfaceSelectionGroupButton
import com.zaneschepke.wireguardautotunnel.ui.screens.main.tunneloptions.components.*
import com.zaneschepke.wireguardautotunnel.viewmodel.AppViewModel

@Composable
fun TunnelOptionsScreen(tunnelConf: TunnelConf, viewModel: AppViewModel) {

    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.Top),
        modifier =
            Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = 24.dp)
                .padding(horizontal = 12.dp),
    ) {
        SurfaceSelectionGroupButton(
            items =
                listOf(
                    PrimaryTunnelItem(tunnelConf, viewModel),
                    AutoTunnelingItem(tunnelConf),
                    ServerIpv4Item(tunnelConf, viewModel),
                    SplitTunnelingItem(tunnelConf),
                )
        )
        SectionDivider()
        SurfaceSelectionGroupButton(
            items =
                buildList {
                    add(PingRestartItem(tunnelConf, viewModel))
                    if (tunnelConf.isPingEnabled) {
                        add(PingConfigItem(tunnelConf, viewModel))
                    }
                }
        )
    }
}
