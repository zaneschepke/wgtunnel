package com.zaneschepke.wireguardautotunnel.ui.common.text

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun SectionTitle(title: String, padding: Dp) {
	Text(
		title,
		textAlign = TextAlign.Start,
		style = MaterialTheme.typography.titleMedium,
		modifier = Modifier.padding(padding, bottom = 5.dp, top = 5.dp),
	)
}
