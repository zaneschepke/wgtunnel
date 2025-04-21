package com.zaneschepke.wireguardautotunnel.ui.common

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SectionDivider() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outline.copy(0.30f),
        modifier = Modifier.padding(horizontal = 12.dp),
    )
}
