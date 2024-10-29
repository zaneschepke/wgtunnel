package com.zaneschepke.wireguardautotunnel.ui.common.button

import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.util.extensions.scaledHeight

@Composable
fun ScaledSwitch(checked: Boolean, onClick: (checked: Boolean) -> Unit, enabled: Boolean = true, modifier: Modifier = Modifier) {
	Switch(
		checked,
		{ onClick(it) },
		modifier.scale((52.dp.scaledHeight() / 52.dp)),
		enabled = enabled,
	)
}
