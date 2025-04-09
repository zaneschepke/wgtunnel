package com.zaneschepke.wireguardautotunnel.ui.screens.autotunnel.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import com.zaneschepke.wireguardautotunnel.R

@Composable
fun WildcardsLabel() {
    Text(
        stringResource(R.string.wildcards_active),
        style =
            MaterialTheme.typography.bodySmall.copy(
                MaterialTheme.colorScheme.outline,
                fontStyle = FontStyle.Italic,
            ),
    )
}
