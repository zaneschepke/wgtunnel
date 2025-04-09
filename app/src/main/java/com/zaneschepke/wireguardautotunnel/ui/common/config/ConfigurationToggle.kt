package com.zaneschepke.wireguardautotunnel.ui.common.config

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.zaneschepke.wireguardautotunnel.ui.common.button.ScaledSwitch

@Composable
fun ConfigurationToggle(
    label: String,
    enabled: Boolean = true,
    checked: Boolean,
    onCheckChanged: (checked: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            label,
            textAlign = TextAlign.Start,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.weight(weight = 1.0f, fill = false),
            softWrap = true,
        )
        ScaledSwitch(
            modifier = modifier,
            enabled = enabled,
            checked = checked,
            onClick = { onCheckChanged(it) },
        )
    }
}
