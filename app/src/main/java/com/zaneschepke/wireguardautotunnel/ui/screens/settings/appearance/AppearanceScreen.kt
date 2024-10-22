package com.zaneschepke.wireguardautotunnel.ui.screens.settings.appearance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Contrast
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.ui.Route
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItem
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SurfaceSelectionGroupButton
import com.zaneschepke.wireguardautotunnel.ui.common.navigation.LocalNavController
import com.zaneschepke.wireguardautotunnel.util.extensions.scaledHeight
import com.zaneschepke.wireguardautotunnel.util.extensions.scaledWidth
import com.zaneschepke.wireguardautotunnel.R

@Composable
fun AppearanceScreen() {
	val navController = LocalNavController.current

	Column(
		horizontalAlignment = Alignment.Start,
		verticalArrangement = Arrangement.spacedBy(24.dp.scaledHeight(), Alignment.Top),
		modifier =
		Modifier
			.fillMaxSize()
			.windowInsetsPadding(WindowInsets.systemBars)
			.padding(top = 24.dp.scaledHeight())
			.padding(horizontal = 24.dp.scaledWidth()),
	) {
		SurfaceSelectionGroupButton(
			listOf(
				SelectionItem(
					Icons.Outlined.Translate,
					title = { Text(stringResource(R.string.language), style = MaterialTheme.typography.bodyLarge.copy(MaterialTheme.colorScheme.onSurface)) },
					onClick = { navController.navigate(Route.Language) },
					trailing = {
						val icon = Icons.AutoMirrored.Outlined.ArrowForward
						Icon(icon, icon.name)
					}
				),
			),
		)
		SurfaceSelectionGroupButton(
			listOf(
				SelectionItem(
					Icons.Outlined.Contrast,
					title = { Text(stringResource(R.string.display_theme), style = MaterialTheme.typography.bodyLarge.copy(MaterialTheme.colorScheme.onSurface)) },
					onClick = { navController.navigate(Route.Display) },
					trailing = {
						val icon = Icons.AutoMirrored.Outlined.ArrowForward
						Icon(icon, icon.name)
					}
				),
			),
		)
	}
}
