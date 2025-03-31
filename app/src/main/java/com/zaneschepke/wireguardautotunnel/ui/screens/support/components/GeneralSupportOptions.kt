package com.zaneschepke.wireguardautotunnel.ui.screens.support.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.ViewTimeline
import androidx.compose.material.icons.outlined.ViewHeadline
import androidx.compose.runtime.Composable
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.Route
import com.zaneschepke.wireguardautotunnel.ui.common.button.ScaledSwitch
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItem
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItemLabel
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SurfaceSelectionGroupButton
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.components.ForwardButton
import com.zaneschepke.wireguardautotunnel.ui.screens.support.SupportViewModel
import com.zaneschepke.wireguardautotunnel.ui.state.AppUiState
import com.zaneschepke.wireguardautotunnel.util.extensions.openWebUrl

@Composable
fun GeneralSupportOptions(
	context: android.content.Context,
	appUiState: AppUiState,
	viewModel: SupportViewModel,
	navController: androidx.navigation.NavController,
	isTv: Boolean,
) {
	SurfaceSelectionGroupButton(
		items = buildList {
			add(
				SelectionItem(
					leadingIcon = Icons.Filled.Book,
					title = { SelectionItemLabel(R.string.docs_description) },
					trailing = { ForwardButton { context.openWebUrl(context.getString(R.string.docs_url)) } },
					onClick = { context.openWebUrl(context.getString(R.string.docs_url)) },
				),
			)
			if (!isTv) {
				add(
					SelectionItem(
						leadingIcon = Icons.Outlined.ViewHeadline,
						title = { SelectionItemLabel(R.string.local_logging) },
						description = { SelectionItemLabel(R.string.enable_local_logging, isDescription = true) },
						trailing = {
							ScaledSwitch(
								checked = appUiState.generalState.isLocalLogsEnabled,
								onClick = { viewModel.onToggleLocalLogging() },
							)
						},
						onClick = { viewModel.onToggleLocalLogging() },
					),
				)
				if (appUiState.generalState.isLocalLogsEnabled) {
					add(
						SelectionItem(
							leadingIcon = Icons.Filled.ViewTimeline,
							title = { SelectionItemLabel(R.string.read_logs) },
							trailing = { ForwardButton { navController.navigate(Route.Logs) } },
							onClick = { navController.navigate(Route.Logs) },
						),
					)
				}
			}
			add(
				SelectionItem(
					leadingIcon = Icons.Filled.Policy,
					title = { SelectionItemLabel(R.string.privacy_policy) },
					trailing = { ForwardButton { context.openWebUrl(context.getString(R.string.privacy_policy_url)) } },
					onClick = { context.openWebUrl(context.getString(R.string.privacy_policy_url)) },
				),
			)
		},
	)
}
