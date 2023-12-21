package com.zaneschepke.wireguardautotunnel.ui.common.prompt

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.WireGuardAutoTunnel

@Composable
fun CustomSnackBar(
    message: String,
    isRtl: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.surface
) {
    val context = LocalContext.current
    Snackbar(
        containerColor = containerColor,
        modifier =
        Modifier.fillMaxWidth(
            if (WireGuardAutoTunnel.isRunningOnAndroidTv(context)) 1 / 3f else 2 / 3f
        ).padding(bottom = 100.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        CompositionLocalProvider(
            LocalLayoutDirection provides
                    if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
        ) {
            Row(
                modifier = Modifier.width(IntrinsicSize.Max).height(IntrinsicSize.Min),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Icon(
                    Icons.Rounded.Info,
                    contentDescription = stringResource(R.string.info),
                    tint = Color.White,
                    modifier = Modifier.padding(end = 10.dp)
                )
                Text(message, color = Color.White, modifier = Modifier.padding(end = 5.dp))
            }
        }
    }
}
