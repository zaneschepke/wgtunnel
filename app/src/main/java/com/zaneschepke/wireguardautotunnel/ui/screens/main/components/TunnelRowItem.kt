package com.zaneschepke.wireguardautotunnel.ui.screens.main.components

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Circle
import androidx.compose.material.icons.rounded.CopyAll
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Settings
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
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.data.domain.TunnelConfig
import com.zaneschepke.wireguardautotunnel.service.tunnel.VpnState
import com.zaneschepke.wireguardautotunnel.ui.Route
import com.zaneschepke.wireguardautotunnel.ui.common.ExpandingRowListItem
import com.zaneschepke.wireguardautotunnel.ui.common.button.ScaledSwitch
import com.zaneschepke.wireguardautotunnel.ui.common.navigation.LocalNavController
import com.zaneschepke.wireguardautotunnel.ui.common.snackbar.SnackbarController
import com.zaneschepke.wireguardautotunnel.util.extensions.asColor
import com.zaneschepke.wireguardautotunnel.util.extensions.isRunningOnTv
import com.zaneschepke.wireguardautotunnel.util.extensions.scaledHeight

@Composable
fun TunnelRowItem(
	isActive: Boolean,
	expanded: Boolean,
	isSelected: Boolean,
	tunnel: TunnelConfig,
	vpnState: VpnState,
	onHold: () -> Unit,
	onClick: () -> Unit,
	onCopy: () -> Unit,
	onDelete: () -> Unit,
	onSwitchClick: (checked: Boolean) -> Unit,
) {
	val leadingIconColor = if (!isActive) Color.Gray else vpnState.statistics.asColor()
	val context = LocalContext.current
	val snackbar = SnackbarController.current
	val navController = LocalNavController.current
	val haptic = LocalHapticFeedback.current
	val itemFocusRequester = remember { FocusRequester() }
	ExpandingRowListItem(
		leading = {
			val circleIcon = Icons.Rounded.Circle
			val icon =
				if (tunnel.isPrimaryTunnel) {
					Icons.Rounded.Star
				} else if (tunnel.isMobileDataTunnel) {
					Icons.Rounded.Smartphone
				} else {
					circleIcon
				}
			Icon(
				icon,
				icon.name,
				tint = leadingIconColor,
				modifier = Modifier.size(16.dp.scaledHeight()),
			)
		},
		text = tunnel.name,
		onHold = {
			haptic.performHapticFeedback(HapticFeedbackType.LongPress)
			onHold()
		},
		onClick = {
			if (!context.isRunningOnTv()) {
				if (isActive) {
					onClick()
				}
			} else {
				onHold()
				itemFocusRequester.requestFocus()
			}
		},
		isExpanded = expanded && isActive,
		expanded = { if (isActive && expanded) TunnelStatisticsRow(vpnState.statistics, tunnel) },
		trailing = {
			if (
				isSelected &&
				!context.isRunningOnTv()
			) {
				Row {
					IconButton(
						onClick = {
							navController.navigate(
								Route.Option(tunnel.id),
							)
						},
					) {
						val icon = Icons.Rounded.Settings
						Icon(
							icon,
							icon.name,
						)
					}
					IconButton(
						modifier = Modifier.focusable(),
						onClick = { onCopy() },
					) {
						val icon = Icons.Rounded.CopyAll
						Icon(icon, icon.name)
					}
					IconButton(
						enabled = !isActive,
						modifier = Modifier.focusable(),
						onClick = { onDelete() },
					) {
						val icon = Icons.Rounded.Delete
						Icon(icon, icon.name)
					}
				}
			} else {
				if (context.isRunningOnTv()) {
					Row {
						IconButton(
							onClick = {
								onHold()
								navController.navigate(
									Route.Option(tunnel.id),
								)
							},
						) {
							val icon = Icons.Rounded.Settings
							Icon(
								icon,
								icon.name,
							)
						}
						IconButton(
							onClick = {
								if (isActive) {
									onClick()
								} else {
									snackbar.showMessage(
										context.getString(R.string.turn_on_tunnel),
									)
								}
							},
						) {
							val icon = Icons.Rounded.Info
							Icon(icon, icon.name)
						}
						IconButton(
							onClick = { onCopy() },
						) {
							val icon = Icons.Rounded.CopyAll
							Icon(icon, icon.name)
						}
						IconButton(
							onClick = {
								if (isActive) {
									snackbar.showMessage(
										context.getString(R.string.turn_off_tunnel),
									)
								} else {
									onHold()
									onDelete()
								}
							},
						) {
							val icon = Icons.Rounded.Delete
							Icon(
								icon,
								icon.name,
							)
						}
						ScaledSwitch(
							modifier = Modifier.focusRequester(itemFocusRequester),
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
			}
		},
	)
}
