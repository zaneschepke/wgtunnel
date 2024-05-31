package com.zaneschepke.wireguardautotunnel.ui.common.config

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp

@Composable
fun ConfigurationToggle(
    label: String,
    enabled: Boolean,
    checked: Boolean,
    padding: Dp,
    onCheckChanged: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(padding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            label, textAlign = TextAlign.Start,
            modifier = Modifier
                .weight(
                    weight = 1.0f,
                    fill = false,
                ),
            softWrap = true,
        )
        Switch(
            modifier = modifier,
            enabled = enabled,
            checked = checked,
            onCheckedChange = { onCheckChanged() },
        )
    }
}
