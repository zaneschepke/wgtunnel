package com.zaneschepke.wireguardautotunnel.ui.common.button.surface

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.ui.theme.iconSize

@Composable
fun SurfaceSelectionGroupButton(items: List<SelectionItem>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        items.map { item ->
            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .then(item.onClick?.let { Modifier.clickable { it() } } ?: Modifier),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(4f, false).fillMaxWidth(),
                    ) {
                        item.leadingIcon?.let { icon ->
                            Icon(
                                icon,
                                icon.name,
                                modifier = Modifier.size(iconSize),
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        Column(
                            horizontalAlignment = Alignment.Start,
                            verticalArrangement =
                                Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
                            modifier =
                                Modifier.fillMaxWidth()
                                    .padding(start = if (item.leadingIcon != null) 16.dp else 0.dp)
                                    .weight(1f)
                                    .padding(
                                        vertical = if (item.description == null) 16.dp else 6.dp
                                    ),
                        ) {
                            item.title()
                            item.description?.let { it() }
                        }
                    }
                    item.trailing?.let {
                        Box(
                            contentAlignment = Alignment.CenterEnd,
                            modifier = Modifier.padding(start = 16.dp),
                        ) {
                            it()
                        }
                    }
                }
            }
        }
    }
}
