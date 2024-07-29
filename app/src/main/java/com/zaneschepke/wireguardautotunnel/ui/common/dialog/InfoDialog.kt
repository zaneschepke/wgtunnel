package com.zaneschepke.wireguardautotunnel.ui.common.dialog

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.zaneschepke.wireguardautotunnel.R

@Composable
fun InfoDialog(onAttest: () -> Unit, onDismiss: () -> Unit, title: @Composable () -> Unit, body: @Composable () -> Unit, confirmText: @Composable () -> Unit) {
	AlertDialog(
		onDismissRequest = { onDismiss() },
		confirmButton = {
			TextButton(
				onClick = {
					onAttest()
				},
			) {
				confirmText()
			}
		},
		dismissButton = {
			TextButton(onClick = { onDismiss() }) {
				Text(text = stringResource(R.string.cancel))
			}
		},
		title = { title() },
		text = { body() },
	)
}
