package com.zaneschepke.wireguardautotunnel.ui.common.button

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.ui.theme.iconSize
import com.zaneschepke.wireguardautotunnel.util.extensions.scaledHeight
import com.zaneschepke.wireguardautotunnel.util.extensions.scaledWidth
import kotlin.let

@androidx.compose.runtime.Composable
fun IconSurfaceButton(title: String, onClick: () -> Unit, selected: Boolean, leadingIcon: ImageVector? = null, description: String? = null) {
	val border: BorderStroke? =
		if (selected) BorderStroke(
            1.dp,
			MaterialTheme.colorScheme.primary
        ) else null
	val interactionSource =
        androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    androidx.compose.material3.Card(
        modifier =
        Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clickable(interactionSource = interactionSource, indication = null) {
                onClick()
            },
        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
        border = border,
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
       Column(
            modifier =
            Modifier
                .padding(horizontal = 8.dp.scaledWidth(), vertical = 10.dp.scaledHeight())
                .padding(end = 16.dp.scaledWidth()).padding(start = 8.dp.scaledWidth())
                .fillMaxSize(),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
            horizontalAlignment = androidx.compose.ui.Alignment.Companion.Start,
        ) {
            androidx.compose.foundation.layout.Row(
                verticalAlignment = androidx.compose.ui.Alignment.Companion.CenterVertically,
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp.scaledWidth()),
            ) {
                androidx.compose.foundation.layout.Row(
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(
                        16.dp.scaledWidth()
                    ),
                    verticalAlignment = androidx.compose.ui.Alignment.Companion.CenterVertically,
                    modifier = Modifier.padding(vertical = if (description == null) 10.dp.scaledHeight() else 0.dp),
                ) {
                    leadingIcon?.let {
                        Icon(
                            leadingIcon,
                            leadingIcon.name,
                            Modifier.Companion.size(iconSize.scaledWidth()),
                            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Column {
                        Text(
                            title,
                            style = MaterialTheme.typography.titleMedium
                        )
                        description?.let {
                            Text(
                                description,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
        }
    }
}
