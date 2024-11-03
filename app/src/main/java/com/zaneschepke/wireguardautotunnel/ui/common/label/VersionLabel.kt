package com.zaneschepke.wireguardautotunnel.ui.common.label

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import com.zaneschepke.wireguardautotunnel.BuildConfig
import com.zaneschepke.wireguardautotunnel.R

@Composable
fun VersionLabel() {
	val clipboardManager = LocalClipboardManager.current
	Row(
		verticalAlignment = Alignment.CenterVertically,
		horizontalArrangement = Arrangement.Start,
	) {
		Text(
			"${stringResource(R.string.version)}: ${BuildConfig.VERSION_NAME}",
			style = MaterialTheme.typography.labelMedium,
			color = MaterialTheme.colorScheme.outline,
			modifier = Modifier.clickable {
				clipboardManager.setText(AnnotatedString(BuildConfig.VERSION_NAME))
			},
		)
	}
}
