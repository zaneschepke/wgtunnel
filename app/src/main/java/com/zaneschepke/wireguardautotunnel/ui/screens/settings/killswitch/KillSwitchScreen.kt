package com.zaneschepke.wireguardautotunnel.ui.screens.settings.killswitch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SurfaceSelectionGroupButton
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.killswitch.components.VpnKillSwitchItem
import com.zaneschepke.wireguardautotunnel.ui.state.AppUiState
import com.zaneschepke.wireguardautotunnel.util.extensions.isRunningOnTv
import com.zaneschepke.wireguardautotunnel.viewmodel.AppViewModel
import com.zaneschepke.wireguardautotunnel.viewmodel.event.AppEvent

@Composable
fun KillSwitchScreen(uiState: AppUiState, viewModel: AppViewModel) {
    val context = LocalContext.current

    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.Top),
        modifier = Modifier.fillMaxSize().padding(vertical = 24.dp).padding(horizontal = 12.dp),
    ) {
        if (!context.isRunningOnTv()) {
            SurfaceSelectionGroupButton(items = listOf(NativeKillSwitchItem()))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(0.30f))
        }
        SurfaceSelectionGroupButton(
            items =
                buildList {
                    if (!uiState.appSettings.isKernelEnabled) {
                        add(
                            VpnKillSwitchItem(uiState) {
                                viewModel.handleEvent(AppEvent.ToggleVpnKillSwitch)
                            }
                        )
                        if (uiState.appSettings.isVpnKillSwitchEnabled) {
                            add(LanTrafficItem(uiState, viewModel))
                        }
                    }
                }
        )
    }
}
