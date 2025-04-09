package com.zaneschepke.wireguardautotunnel.ui.common.button.surface

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

@Composable
fun SelectionItemLabel(
    textResId: Int,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyMedium,
    isDescription: Boolean = false,
) {
    Text(
        text = stringResource(textResId),
        style =
            style.copy(
                color =
                    if (isDescription) MaterialTheme.colorScheme.outline
                    else MaterialTheme.colorScheme.onSurface
            ),
    )
}
