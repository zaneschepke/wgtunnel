package com.zaneschepke.wireguardautotunnel.ui.screens.settings.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.zaneschepke.wireguardautotunnel.R

@Composable
fun LocationServicesDialog(show: Boolean, onDismiss: () -> Unit, onAttest: () -> Unit) {
    if (show) {
        AlertDialog(
            onDismissRequest = { onDismiss() },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDismiss()
                        onAttest()
                    }
                ) {
                    Text(text = stringResource(R.string.okay))
                }
            },
            dismissButton = {
                TextButton(onClick = { onDismiss() }) {
                    Text(text = stringResource(R.string.cancel))
                }
            },
            title = { Text(text = stringResource(R.string.location_services_not_detected)) },
            text = { Text(text = stringResource(R.string.location_services_missing_message)) },
        )
    }
}
