package com.zaneschepke.wireguardautotunnel.ui.screens.tunneloptions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.CallSplit
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.AppUiState
import com.zaneschepke.wireguardautotunnel.ui.Route
import com.zaneschepke.wireguardautotunnel.ui.common.button.ScaledSwitch
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItem
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SurfaceSelectionGroupButton
import com.zaneschepke.wireguardautotunnel.ui.common.navigation.LocalNavController
import com.zaneschepke.wireguardautotunnel.ui.common.navigation.TopNavBar
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.components.ForwardButton
import com.zaneschepke.wireguardautotunnel.util.extensions.scaledHeight
import com.zaneschepke.wireguardautotunnel.util.extensions.scaledWidth

@Composable
fun OptionsScreen(tunnelOptionsViewModel: TunnelOptionsViewModel = hiltViewModel(), appUiState: AppUiState, tunnelId: Int) {
	val navController = LocalNavController.current
	val config = appUiState.tunnels.first { it.id == tunnelId }

	var currentText by remember { mutableStateOf("") }

	LaunchedEffect(config.tunnelNetworks) {
		currentText = ""
	}
	Scaffold(
		topBar = {
			TopNavBar(config.name)
		},
	) {
		Column(
			horizontalAlignment = Alignment.Start,
			verticalArrangement = Arrangement.spacedBy(24.dp.scaledHeight(), Alignment.Top),
			modifier =
			Modifier
				.fillMaxSize()
				.padding(it)
				.verticalScroll(rememberScrollState())
				.padding(top = 24.dp.scaledHeight())
				.padding(horizontal = 24.dp.scaledWidth()),
		) {
			SurfaceSelectionGroupButton(
				listOf(
					SelectionItem(
						Icons.Outlined.Star,
						title = {
							Text(
								stringResource(R.string.primary_tunnel),
								style = MaterialTheme.typography.bodyMedium.copy(MaterialTheme.colorScheme.onSurface),
							)
						},
						description = {
							Text(
								stringResource(R.string.set_primary_tunnel),
								style = MaterialTheme.typography.bodySmall.copy(MaterialTheme.colorScheme.outline),
							)
						},
						trailing = {
							ScaledSwitch(
								config.isPrimaryTunnel,
								onClick = { tunnelOptionsViewModel.onTogglePrimaryTunnel(config) },
							)
						},
						onClick = { tunnelOptionsViewModel.onTogglePrimaryTunnel(config) },
					),
					SelectionItem(
						Icons.Outlined.Bolt,
						title = {
							Text(
								stringResource(R.string.auto_tunneling),
								style = MaterialTheme.typography.bodyMedium.copy(MaterialTheme.colorScheme.onSurface),
							)
						},
						description = {
							Text(
								stringResource(R.string.tunnel_specific_settings),
								style = MaterialTheme.typography.bodySmall.copy(MaterialTheme.colorScheme.outline),
							)
						},
						onClick = {
							navController.navigate(Route.TunnelAutoTunnel(id = tunnelId))
						},
						trailing = {
							ForwardButton { navController.navigate(Route.TunnelAutoTunnel(id = tunnelId)) }
						},
					),
					SelectionItem(
						Icons.Outlined.Edit,
						title = {
							Text(
								stringResource(R.string.edit_tunnel),
								style = MaterialTheme.typography.bodyMedium.copy(MaterialTheme.colorScheme.onSurface),
							)
						},
						onClick = {
							navController.navigate(Route.Config(id = tunnelId))
						},
						trailing = {
							ForwardButton { navController.navigate(Route.Config(id = tunnelId)) }
						},
					),
					SelectionItem(
						Icons.AutoMirrored.Outlined.CallSplit,
						title = {
							Text(
								stringResource(R.string.splt_tunneling),
								style = MaterialTheme.typography.bodyMedium.copy(MaterialTheme.colorScheme.onSurface),
							)
						},
						onClick = {
							navController.navigate(Route.SplitTunnel(id = tunnelId))
						},
						trailing = {
							ForwardButton { navController.navigate(Route.SplitTunnel(id = tunnelId)) }
						},
					),
				),
			)
// 			GroupLabel(stringResource(R.string.quick_actions))
		}
	}
}
