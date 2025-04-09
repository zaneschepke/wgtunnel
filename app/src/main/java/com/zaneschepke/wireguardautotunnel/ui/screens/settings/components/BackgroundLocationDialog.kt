package com.zaneschepke.wireguardautotunnel.ui.screens.settings.components

import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.common.dialog.InfoDialog
import com.zaneschepke.wireguardautotunnel.util.extensions.launchAppSettings

@Composable
fun BackgroundLocationDialog(show: Boolean, onDismiss: () -> Unit, onAttest: () -> Unit) {
    val context = LocalContext.current
    if (show) {
        val alwaysOnDescription = buildAnnotatedString {
            append(stringResource(R.string.background_location_message))
            append(" ")
            pushStringAnnotation(tag = "appSettings", annotation = "")
            withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                append(stringResource(id = R.string.app_settings))
            }
            pop()
            append(" ")
            append(stringResource(R.string.background_location_message2))
            append(".")
        }
        InfoDialog(
            onDismiss = { onDismiss() },
            onAttest = { onDismiss() },
            title = { Text(text = stringResource(R.string.vpn_denied_dialog_title)) },
            body = {
                ClickableText(
                    text = alwaysOnDescription,
                    style =
                        MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.outline
                        ),
                ) {
                    alwaysOnDescription
                        .getStringAnnotations(tag = "appSettings", it, it)
                        .firstOrNull()
                        ?.let { context.launchAppSettings() }
                }
            },
            confirmText = { Text(text = stringResource(R.string.okay)) },
        )
    }
}
