package com.zaneschepke.wireguardautotunnel.ui.screens.main.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Circle
import androidx.compose.material.icons.rounded.SettingsEthernet
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.domain.entity.TunnelConf
import com.zaneschepke.wireguardautotunnel.domain.state.TunnelState
import com.zaneschepke.wireguardautotunnel.ui.common.ExpandingRowListItem
import com.zaneschepke.wireguardautotunnel.ui.common.button.ScaledSwitch
import com.zaneschepke.wireguardautotunnel.util.extensions.asColor

@Composable
fun TunnelRowItem(
    state: TunnelState,
    isSelected: Boolean,
    expanded: Boolean,
    tunnel: TunnelConf,
    tunnelState: TunnelState,
    onClick: () -> Unit,
    onDoubleClick: () -> Unit,
    onToggleSelectedTunnel: (TunnelConf) -> Unit,
    onSwitchClick: (Boolean) -> Unit,
) {
    val itemFocusRequester = remember { FocusRequester() }

    val leadingIconColor =
        remember(state) {
            if (state.status.isUp()) tunnelState.statistics.asColor() else Color.Gray
        }

    val (leadingIcon, size) =
        remember(tunnel) {
            when {
                tunnel.isPrimaryTunnel -> Pair(Icons.Rounded.Star, 16.dp)
                tunnel.isMobileDataTunnel -> Pair(Icons.Rounded.Smartphone, 16.dp)
                tunnel.isEthernetTunnel -> Pair(Icons.Rounded.SettingsEthernet, 16.dp)
                else -> Pair(Icons.Rounded.Circle, 14.dp)
            }
        }

    ExpandingRowListItem(
        leading = {
            Icon(
                leadingIcon,
                stringResource(R.string.status),
                tint = leadingIconColor,
                modifier = Modifier.size(size),
            )
        },
        text = tunnel.tunName,
        onHold = { onToggleSelectedTunnel(tunnel) },
        onClick = onClick,
        onDoubleClick = onDoubleClick,
        expanded = {
            if (expanded) {
                TunnelStatisticsRow(tunnelState.statistics, tunnel)
            } else null
        },
        trailing = {
            ScaledSwitch(
                modifier = Modifier.focusRequester(itemFocusRequester),
                checked = state.status.isUpOrStarting(),
                onClick = onSwitchClick,
            )
        },
        isSelected = isSelected,
    )
}
