package com.zaneschepke.wireguardautotunnel.ui.screens.main.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.common.navigation.TopNavBar

@Composable
fun AddTunnelFab(isVisible: Boolean = true, isTv: Boolean = false, onClick: () -> Unit) {
	if (isTv) {
		TopNavBar(
			showBack = false,
			title = stringResource(R.string.app_name),
			trailing = {
				IconButton(onClick = onClick) {
					Icon(Icons.Outlined.Add, stringResource(R.string.add_tunnel))
				}
			},
		)
	} else {
		ScrollDismissFab(
			icon = {
				Icon(
					Icons.Filled.Add,
					stringResource(R.string.add_tunnel),
					tint = MaterialTheme.colorScheme.onPrimary,
				)
			},
			isVisible = isVisible,
			onClick = onClick,
		)
	}
}
