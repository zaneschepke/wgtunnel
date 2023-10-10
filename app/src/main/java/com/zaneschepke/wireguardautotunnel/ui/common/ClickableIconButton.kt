package com.zaneschepke.wireguardautotunnel.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.zaneschepke.wireguardautotunnel.R

@Composable
fun ClickableIconButton(onIconClick : () -> Unit, text : String, icon : ImageVector, enabled : Boolean) {
    TextButton(onClick = {},
        enabled = enabled
    ) {
        Text(text)
        Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
        Icon(
            imageVector = icon,
            contentDescription = stringResource(R.string.delete),
            modifier = Modifier.size(ButtonDefaults.IconSize).clickable {
                if(enabled) {
                    onIconClick()
                }
            }
        )
    }
}