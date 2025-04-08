package com.zaneschepke.wireguardautotunnel.ui.screens.main.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.overscroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.core.tunnel.getValueById
import com.zaneschepke.wireguardautotunnel.domain.entity.TunnelConf
import com.zaneschepke.wireguardautotunnel.domain.state.TunnelState
import com.zaneschepke.wireguardautotunnel.ui.state.AppUiState
import com.zaneschepke.wireguardautotunnel.util.extensions.openWebUrl
import com.zaneschepke.wireguardautotunnel.viewmodel.AppViewModel
import java.text.Collator
import java.util.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TunnelList(
	appUiState: AppUiState,
	activeTunnels: Map<TunnelConf, TunnelState>,
	selectedTunnel: TunnelConf?,
	onSetSelectedTunnel: (TunnelConf?) -> Unit,
	onDeleteTunnel: (TunnelConf) -> Unit,
	onToggleTunnel: (TunnelConf, Boolean) -> Unit,
	onExpandStats: () -> Unit,
	onCopyTunnel: (TunnelConf) -> Unit,
	modifier: Modifier = Modifier,
	viewModel: AppViewModel,
) {
	val context = LocalContext.current
	val collator = Collator.getInstance(Locale.getDefault())
	val sortedTunnels = remember(appUiState.tunnels) {
		appUiState.tunnels.sortedWith(compareBy(collator) { it.tunName })
	}

	LazyColumn(
		horizontalAlignment = Alignment.Start,
		verticalArrangement = Arrangement.spacedBy(5.dp, Alignment.Top),
		modifier = modifier
			.pointerInput(Unit) {
				if (appUiState.tunnels.isEmpty()) return@pointerInput
			}.overscroll(ScrollableDefaults.overscrollEffect()),
		state = rememberLazyListState(0, appUiState.tunnels.count()),
		userScrollEnabled = true,
		reverseLayout = false,
		flingBehavior = ScrollableDefaults.flingBehavior(),
	) {
		if (appUiState.tunnels.isEmpty()) {
			item {
				GettingStartedLabel(onClick = { context.openWebUrl(it) })
			}
		}
		items(sortedTunnels, key = { it.id }) { tunnel ->
			val tunnelState = activeTunnels.getValueById(tunnel.id) ?: TunnelState()
			TunnelRowItem(
				isActive = tunnelState.status.isUpOrStarting(),
				expanded = appUiState.appState.isTunnelStatsExpanded,
				isSelected = selectedTunnel?.id == tunnel.id,
				tunnel = tunnel,
				tunnelState = tunnelState,
				onSetSelectedTunnel = { onSetSelectedTunnel(it) },
				onClick = onExpandStats,
				onCopy = { onCopyTunnel(tunnel) },
				onDelete = { onDeleteTunnel(tunnel) },
				onSwitchClick = { checked -> onToggleTunnel(tunnel, checked) },
				viewModel = viewModel,
			)
		}
	}
}
