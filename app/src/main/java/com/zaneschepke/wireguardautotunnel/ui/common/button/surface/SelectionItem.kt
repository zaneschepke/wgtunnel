package com.zaneschepke.wireguardautotunnel.ui.common.button.surface

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

data class SelectionItem(
    val leadingIcon: ImageVector? = null,
    val trailing: (@Composable () -> Unit)? = null,
    val title: (@Composable () -> Unit),
    val description: (@Composable () -> Unit)? = null,
    val onClick: (() -> Unit)? = null,
    val height: Int = 64,
)
