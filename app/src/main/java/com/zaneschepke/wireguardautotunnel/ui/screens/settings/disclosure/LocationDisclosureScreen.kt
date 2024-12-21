package com.zaneschepke.wireguardautotunnel.ui.screens.settings.disclosure

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.rounded.PermScanWifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.AppUiState
import com.zaneschepke.wireguardautotunnel.ui.AppViewModel
import com.zaneschepke.wireguardautotunnel.ui.Route
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItem
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SurfaceSelectionGroupButton
import com.zaneschepke.wireguardautotunnel.ui.common.navigation.LocalNavController
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.components.ForwardButton
import com.zaneschepke.wireguardautotunnel.util.extensions.goFromRoot
import com.zaneschepke.wireguardautotunnel.util.extensions.launchAppSettings
import com.zaneschepke.wireguardautotunnel.util.extensions.scaledHeight
import com.zaneschepke.wireguardautotunnel.util.extensions.scaledWidth

@Composable
fun LocationDisclosureScreen(appViewModel: AppViewModel, appUiState: AppUiState) {
	val context = LocalContext.current
	val navController = LocalNavController.current

	LaunchedEffect(Unit, appUiState) {
		if (appUiState.generalState.isLocationDisclosureShown) navController.goFromRoot(Route.AutoTunnel)
	}

	Column(
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.spacedBy(24.dp.scaledHeight(), Alignment.Top),
		modifier =
		Modifier.fillMaxSize().systemBarsPadding().padding(top = 24.dp.scaledHeight())
			.padding(horizontal = 24.dp.scaledWidth()),
	) {
		val icon = Icons.Rounded.PermScanWifi
		Icon(
			icon,
			contentDescription = icon.name,
			modifier = Modifier
				.padding(30.dp.scaledHeight())
				.size(128.dp.scaledHeight()),
		)
		Text(
			stringResource(R.string.prominent_background_location_title),
			style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
		)
		Text(
			stringResource(R.string.prominent_background_location_message),
			style = MaterialTheme.typography.bodyLarge,
		)
		SurfaceSelectionGroupButton(
			listOf(
				SelectionItem(
					Icons.Outlined.LocationOn,
					title = {
						Text(
							stringResource(R.string.launch_app_settings),
							style = MaterialTheme.typography.bodyLarge.copy(MaterialTheme.colorScheme.onSurface),
						)
					},
					onClick = {
						context.launchAppSettings().also {
							appViewModel.setLocationDisclosureShown()
						}
					},
					trailing = {
						ForwardButton {
							context.launchAppSettings().also {
								appViewModel.setLocationDisclosureShown()
							}
						}
					},
				),
			),
		)
		SurfaceSelectionGroupButton(
			listOf(
				SelectionItem(
					title = { Text(stringResource(R.string.skip), style = MaterialTheme.typography.bodyLarge.copy(MaterialTheme.colorScheme.onSurface)) },
					onClick = { appViewModel.setLocationDisclosureShown() },
					trailing = {
						ForwardButton { appViewModel.setLocationDisclosureShown() }
					},
				),
			),
		)
	}
}
