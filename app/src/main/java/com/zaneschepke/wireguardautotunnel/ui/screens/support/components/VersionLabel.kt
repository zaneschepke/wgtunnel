package com.zaneschepke.wireguardautotunnel.ui.screens.support.components

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
import com.zaneschepke.wireguardautotunnel.util.Constants

@Composable
fun VersionLabel() {
    val clipboardManager = LocalClipboardManager.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Start) {
        val versionText =
            if (BuildConfig.BUILD_TYPE == Constants.RELEASE) {
                BuildConfig.VERSION_NAME
            } else {
                "${BuildConfig.VERSION_NAME}-${BuildConfig.BUILD_TYPE}"
            }
        Text(
            "${stringResource(R.string.version)}: $versionText",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.outline,
            modifier =
                Modifier.clickable {
                    clipboardManager.setText(AnnotatedString(BuildConfig.VERSION_NAME))
                },
        )
    }
}
