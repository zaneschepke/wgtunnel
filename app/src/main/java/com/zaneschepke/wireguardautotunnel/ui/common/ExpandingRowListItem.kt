package com.zaneschepke.wireguardautotunnel.ui.common

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.ui.navigation.LocalIsAndroidTV

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExpandingRowListItem(
    leading: @Composable () -> Unit,
    text: String,
    onHold: () -> Unit,
    onClick: () -> Unit,
    onDoubleClick: () -> Unit,
    trailing: @Composable () -> Unit,
    isSelected: Boolean,
    expanded: (@Composable () -> Unit)?,
) {
    val isTv = LocalIsAndroidTV.current
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier =
            Modifier.animateContentSize()
                .clip(RoundedCornerShape(8.dp))
                .then(
                    if (!isTv) {
                        Modifier.combinedClickable(
                                onClick = onClick,
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onHold()
                                },
                                onDoubleClick = onDoubleClick,
                            )
                            .indication(
                                interactionSource = interactionSource,
                                indication = ripple(),
                            )
                    } else Modifier
                )
    ) {
        LaunchedEffect(isSelected) {
            if (isSelected) {
                interactionSource.emit(PressInteraction.Press(Offset.Zero))
            } else {
                interactionSource.emit(
                    PressInteraction.Release(PressInteraction.Press(Offset.Zero))
                )
            }
        }
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth(13 / 20f),
                ) {
                    leading()
                    Text(
                        text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
                trailing()
            }
            expanded?.invoke()
        }
    }
}
