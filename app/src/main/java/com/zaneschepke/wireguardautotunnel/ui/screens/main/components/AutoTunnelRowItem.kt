package com.zaneschepke.wireguardautotunnel.ui.screens.main.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.data.domain.Settings
import com.zaneschepke.wireguardautotunnel.ui.common.ExpandingRowListItem
import com.zaneschepke.wireguardautotunnel.ui.theme.SilverTree
import com.zaneschepke.wireguardautotunnel.ui.theme.iconSize
import com.zaneschepke.wireguardautotunnel.util.extensions.isRunningOnTv

@Composable
fun AutoTunnelRowItem(settings: Settings, onToggle: () -> Unit, focusRequester: FocusRequester) {
	val context = LocalContext.current
	val itemFocusRequester = remember { FocusRequester() }
	val autoTunnelingLabel =
		buildAnnotatedString {
			append(stringResource(id = R.string.auto_tunneling))
			append(": ")
			if (settings.isAutoTunnelPaused) {
				append(
					stringResource(id = R.string.paused),
				)
			} else {
				append(
					stringResource(id = R.string.active),
				)
			}
		}
	ExpandingRowListItem(
		leading = {
			val icon = Icons.Rounded.Bolt
			Icon(
				icon,
				icon.name,
				modifier =
				Modifier
					.size(iconSize).scale(1.5f),
				tint =
				if (settings.isAutoTunnelPaused) {
					Color.Gray
				} else {
					SilverTree
				},
			)
		},
		text = autoTunnelingLabel.text,
		trailing = {
			TextButton(
				modifier = Modifier.focusRequester(itemFocusRequester),
				onClick = { onToggle() },
			) {
				Text(stringResource(id = if (settings.isAutoTunnelPaused) R.string.resume else R.string.pause))
			}
		},
		onClick = {
			if (context.isRunningOnTv()) {
				itemFocusRequester.requestFocus()
			}
		},
		isExpanded = false,
		focusRequester = focusRequester,
	)
}
