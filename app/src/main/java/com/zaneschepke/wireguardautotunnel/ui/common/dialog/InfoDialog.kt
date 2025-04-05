package com.zaneschepke.wireguardautotunnel.ui.common.dialog

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.zaneschepke.wireguardautotunnel.R
import timber.log.Timber

@Composable
fun InfoDialog(
	onAttest: () -> Unit,
	onDismiss: () -> Unit,
	title: @Composable () -> Unit,
	body: @Composable () -> Unit,
	confirmText: @Composable () -> Unit,
) {
	val isSystemInDarkTheme = isSystemInDarkTheme()
	val color = MaterialTheme.colorScheme.surface

	LaunchedEffect(Unit) {
		Timber.d("Dialog Surface Color: ${color.toArgb()}")
		Timber.d("Dark mode: $isSystemInDarkTheme")
	}
	MaterialTheme(
		colorScheme = MaterialTheme.colorScheme.copy(), // Clone the current theme to avoid overrides
	) {
		Surface(
			color = MaterialTheme.colorScheme.surface, // Use the theme's surface color
			tonalElevation = 0.dp, // Disable elevation tinting to keep color consistent
		) {
			AlertDialog(
				onDismissRequest = { onDismiss() },
				confirmButton = {
					TextButton(onClick = { onAttest() }) {
						confirmText()
					}
				},
				dismissButton = {
					TextButton(onClick = { onDismiss() }) {
						Text(text = stringResource(R.string.cancel))
					}
				},
				containerColor = MaterialTheme.colorScheme.surface,
				title = { title() },
				text = { body() },
				properties = DialogProperties(
					usePlatformDefaultWidth = true,
				),
			)
		}
	}
}
