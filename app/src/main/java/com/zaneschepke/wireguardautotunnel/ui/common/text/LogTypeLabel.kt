package com.zaneschepke.wireguardautotunnel.ui.common.text

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun LogTypeLabel(color: Color, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.size(20.dp).clip(RoundedCornerShape(2.dp)).background(color),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
