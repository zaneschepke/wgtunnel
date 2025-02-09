package com.zaneschepke.wireguardautotunnel.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Contrast
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.Route
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItem
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SurfaceSelectionGroupButton
import com.zaneschepke.wireguardautotunnel.ui.common.navigation.LocalNavController
import com.zaneschepke.wireguardautotunnel.ui.common.navigation.TopNavBar
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.components.ForwardButton
import com.zaneschepke.wireguardautotunnel.util.extensions.launchNotificationSettings
import com.zaneschepke.wireguardautotunnel.util.extensions.scaledHeight
import com.zaneschepke.wireguardautotunnel.util.extensions.scaledWidth

@Composable
fun AppearanceScreen() {
	val navController = LocalNavController.current
	val context = LocalContext.current

	Scaffold(
		topBar = {
			TopNavBar(stringResource(R.string.appearance))
		},
	) {
		Column(
			horizontalAlignment = Alignment.Start,
			verticalArrangement = Arrangement.spacedBy(24.dp.scaledHeight(), Alignment.Top),
			modifier =
			Modifier
				.fillMaxSize().padding(it)
				.padding(top = 24.dp.scaledHeight())
				.padding(horizontal = 24.dp.scaledWidth()),
		) {
			SurfaceSelectionGroupButton(
				listOf(
					SelectionItem(
						Icons.Outlined.Translate,
						title = { Text(stringResource(R.string.language), style = MaterialTheme.typography.bodyMedium.copy(MaterialTheme.colorScheme.onSurface)) },
						onClick = { navController.navigate(Route.Language) },
						trailing = {
							ForwardButton { navController.navigate(Route.Language) }
						},
					),
				),
			)
			SurfaceSelectionGroupButton(
				listOf(
					SelectionItem(
						Icons.Outlined.Notifications,
						title = { Text(stringResource(R.string.notifications), style = MaterialTheme.typography.bodyMedium.copy(MaterialTheme.colorScheme.onSurface)) },
						onClick = {
							context.launchNotificationSettings()
						},
						trailing = {
							ForwardButton { context.launchNotificationSettings() }
						},
					),
				),
			)
			SurfaceSelectionGroupButton(
				listOf(
					SelectionItem(
						Icons.Outlined.Contrast,
						title = { Text(stringResource(R.string.display_theme), style = MaterialTheme.typography.bodyMedium.copy(MaterialTheme.colorScheme.onSurface)) },
						onClick = { navController.navigate(Route.Display) },
						trailing = {
							ForwardButton { navController.navigate(Route.Display) }
						},
					),
				),
			)
		}
	}
}
