package com.zaneschepke.wireguardautotunnel.ui.common.dialog

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
import com.zaneschepke.wireguardautotunnel.util.extensions.launchVpnSettings

@Composable
fun VpnDeniedDialog(show: Boolean, onDismiss: () -> Unit) {
    val context = LocalContext.current
    if (show) {
        val alwaysOnDescription = buildAnnotatedString {
            append(stringResource(R.string.always_on_message))
            append(" ")
            pushStringAnnotation(tag = "vpnSettings", annotation = "")
            withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                append(stringResource(id = R.string.vpn_settings))
            }
            pop()
            append(" ")
            append(stringResource(R.string.always_on_message2))
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
                        .getStringAnnotations(tag = "vpnSettings", it, it)
                        .firstOrNull()
                        ?.let { context.launchVpnSettings() }
                }
            },
            confirmText = { Text(text = stringResource(R.string.okay)) },
        )
    }
}
