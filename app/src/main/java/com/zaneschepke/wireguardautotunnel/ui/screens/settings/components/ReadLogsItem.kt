package com.zaneschepke.wireguardautotunnel.ui.screens.settings.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ViewTimeline
import androidx.compose.runtime.Composable
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.Route
import com.zaneschepke.wireguardautotunnel.ui.common.button.ForwardButton
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItem
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItemLabel
import com.zaneschepke.wireguardautotunnel.ui.common.navigation.LocalNavController

@Composable
fun ReadLogsItem(): SelectionItem {
	val navController = LocalNavController.current
	return SelectionItem(
		leadingIcon = Icons.Filled.ViewTimeline,
		title = { SelectionItemLabel(R.string.read_logs) },
		trailing = { ForwardButton { navController.navigate(Route.Logs) } },
		onClick = { navController.navigate(Route.Logs) },
	)
}
