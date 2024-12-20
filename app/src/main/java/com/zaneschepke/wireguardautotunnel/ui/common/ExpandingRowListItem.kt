package com.zaneschepke.wireguardautotunnel.ui.common

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExpandingRowListItem(
	leading: @Composable () -> Unit,
	text: String,
	onHold: () -> Unit = {},
	onClick: () -> Unit,
	trailing: @Composable () -> Unit,
	isExpanded: Boolean,
	expanded: @Composable () -> Unit = {},
) {
	Box(
		modifier =
		Modifier
			.animateContentSize()
			.clip(RoundedCornerShape(30.dp))
			.combinedClickable(
				onClick = { onClick() },
				onLongClick = { onHold() },
			),
	) {
		Column {
			Row(
				modifier =
				Modifier
					.fillMaxWidth()
					.padding(horizontal = 15.dp),
				verticalAlignment = Alignment.CenterVertically,
				horizontalArrangement = Arrangement.SpaceBetween,
			) {
				Row(
					verticalAlignment = Alignment.CenterVertically,
					horizontalArrangement = Arrangement.spacedBy(15.dp),
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
			if (isExpanded) expanded()
		}
	}
}
