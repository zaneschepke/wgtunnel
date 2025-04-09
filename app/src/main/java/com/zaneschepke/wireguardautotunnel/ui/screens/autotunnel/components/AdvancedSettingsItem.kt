package com.zaneschepke.wireguardautotunnel.ui.screens.autotunnel.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.common.button.ForwardButton
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItem

@Composable
fun AdvancedSettingsItem(onClick: () -> Unit): SelectionItem {
    return SelectionItem(
        leadingIcon = Icons.Outlined.Settings,
        title = {
            Text(
                stringResource(R.string.advanced_settings),
                style =
                    MaterialTheme.typography.bodyMedium.copy(MaterialTheme.colorScheme.onSurface),
            )
        },
        trailing = { ForwardButton { onClick() } },
        onClick = { onClick() },
    )
}
