package com.zaneschepke.wireguardautotunnel.ui.screens.main.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Circle
import androidx.compose.material.icons.rounded.CopyAll
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SettingsEthernet
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.domain.entity.TunnelConf
import com.zaneschepke.wireguardautotunnel.domain.state.TunnelState
import com.zaneschepke.wireguardautotunnel.ui.Route
import com.zaneschepke.wireguardautotunnel.ui.common.ExpandingRowListItem
import com.zaneschepke.wireguardautotunnel.ui.common.button.ScaledSwitch
import com.zaneschepke.wireguardautotunnel.ui.common.navigation.LocalNavController
import com.zaneschepke.wireguardautotunnel.util.StringValue
import com.zaneschepke.wireguardautotunnel.util.extensions.asColor
import com.zaneschepke.wireguardautotunnel.util.extensions.isRunningOnTv
import com.zaneschepke.wireguardautotunnel.viewmodel.AppViewModel
import com.zaneschepke.wireguardautotunnel.viewmodel.event.AppEvent

@Composable
fun TunnelRowItem(
	isActive: Boolean,
	expanded: Boolean,
	isSelected: Boolean,
	tunnel: TunnelConf,
	tunnelState: TunnelState,
	onSetSelectedTunnel: (TunnelConf?) -> Unit,
	onClick: () -> Unit,
	onCopy: () -> Unit,
	onDelete: () -> Unit,
	onSwitchClick: (Boolean) -> Unit,
	viewModel: AppViewModel,
) {
	val context = LocalContext.current
	val navController = LocalNavController.current
	val haptic = LocalHapticFeedback.current
	val itemFocusRequester = remember { FocusRequester() }
	val isTv = context.isRunningOnTv()

	val leadingIconColor = if (!isActive) Color.Gray else tunnelState.statistics.asColor()
	val leadingIcon = when {
		tunnel.isPrimaryTunnel -> Icons.Rounded.Star
		tunnel.isMobileDataTunnel -> Icons.Rounded.Smartphone
		tunnel.isEthernetTunnel -> Icons.Rounded.SettingsEthernet
		else -> Icons.Rounded.Circle
	}

	ExpandingRowListItem(
		leading = {
			Icon(
				leadingIcon,
				leadingIcon.name,
				tint = leadingIconColor,
				modifier = Modifier.size(16.dp),
			)
		},
		text = tunnel.tunName,
		onHold = {
			haptic.performHapticFeedback(HapticFeedbackType.LongPress)
			onSetSelectedTunnel(tunnel)
		},
		onClick = {
			if (!isTv) {
				if (isActive) onClick()
			} else {
				onSetSelectedTunnel(tunnel)
				itemFocusRequester.requestFocus()
			}
		},
		isExpanded = expanded && isActive,
		expanded = { if (isActive && expanded) TunnelStatisticsRow(tunnelState.statistics, tunnel) },
		trailing = {
			if (isSelected && !isTv) {
				Row(
					modifier = Modifier.fillMaxWidth(),
					horizontalArrangement = Arrangement.SpaceEvenly,
				) {
					IconButton(
						modifier = Modifier.weight(1f),
						onClick = {
							onSetSelectedTunnel(null)
							navController.navigate(Route.TunnelOptions(tunnel.id))
						},
					) {
						Icon(
							Icons.Rounded.Settings,
							stringResource(id = R.string.settings),
							modifier = Modifier.size(24.dp),
						)
					}
					IconButton(
						modifier = Modifier.weight(1f),
						onClick = {
							onCopy()
							onSetSelectedTunnel(null)
						},
					) {
						Icon(
							Icons.Rounded.CopyAll,
							stringResource(R.string.copy),
							modifier = Modifier.size(24.dp),
						)
					}
					IconButton(
						modifier = Modifier.weight(1f),
						enabled = !isActive,
						onClick = { onDelete() },
					) {
						Icon(
							Icons.Rounded.Delete,
							stringResource(R.string.delete_tunnel),
							modifier = Modifier.size(24.dp),
						)
					}
				}
			} else if (isTv) {
				Row(
					modifier = Modifier.fillMaxWidth(),
					horizontalArrangement = Arrangement.SpaceEvenly,
				) {
					IconButton(
						modifier = Modifier.weight(1f),
						onClick = {
							navController.navigate(Route.TunnelOptions(tunnel.id))
							onSetSelectedTunnel(null)
						},
					) {
						Icon(
							Icons.Rounded.Settings,
							stringResource(id = R.string.settings),
							modifier = Modifier.size(24.dp),
						)
					}
					IconButton(
						modifier = Modifier.weight(1f),
						onClick = {
							if (isActive) {
								onClick()
							} else {
								viewModel.handleEvent(
									AppEvent.ShowMessage(StringValue.StringResource(R.string.turn_on_tunnel)),
								)
							}
						},
					) {
						Icon(
							Icons.Rounded.Info,
							stringResource(R.string.info),
							modifier = Modifier.size(24.dp),
						)
					}
					IconButton(
						modifier = Modifier.weight(1f),
						onClick = onCopy,
					) {
						Icon(
							Icons.Rounded.CopyAll,
							stringResource(R.string.copy),
							modifier = Modifier.size(24.dp),
						)
					}
					IconButton(
						modifier = Modifier.weight(1f),
						onClick = {
							if (isActive) {
								viewModel.handleEvent(
									AppEvent.ShowMessage(StringValue.StringResource(R.string.turn_off_tunnel)),
								)
							} else {
								onDelete()
							}
						},
					) {
						Icon(
							Icons.Rounded.Delete,
							stringResource(R.string.delete_tunnel),
							modifier = Modifier.size(24.dp),
						)
					}
					ScaledSwitch(
						modifier = Modifier
							.focusRequester(itemFocusRequester)
							.weight(1f),
						checked = isActive,
						onClick = onSwitchClick,
					)
				}
			} else {
				ScaledSwitch(
					modifier = Modifier.focusRequester(itemFocusRequester),
					checked = isActive,
					onClick = onSwitchClick,
				)
			}
		},
	)
}
