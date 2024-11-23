package com.zaneschepke.wireguardautotunnel.ui.common.button

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.ui.theme.iconSize
import com.zaneschepke.wireguardautotunnel.util.extensions.scaledHeight
import com.zaneschepke.wireguardautotunnel.util.extensions.scaledWidth

@androidx.compose.runtime.Composable
fun IconSurfaceButton(title: String, onClick: () -> Unit, selected: Boolean, leadingIcon: ImageVector? = null, description: String? = null) {
	val border: BorderStroke? =
		if (selected) {
			BorderStroke(
				1.dp,
				MaterialTheme.colorScheme.primary,
			)
		} else {
			null
		}
	Card(
		modifier =
		Modifier
			.fillMaxWidth()
			.height(IntrinsicSize.Min),
		shape = RoundedCornerShape(8.dp),
		border = border,
		colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
	) {
		Box(
			modifier = Modifier.clickable { onClick() }
				.fillMaxWidth(),
		) {
			Column(
				modifier =
				Modifier
					.padding(horizontal = 8.dp.scaledWidth(), vertical = 10.dp.scaledHeight())
					.padding(end = 16.dp.scaledWidth()).padding(start = 8.dp.scaledWidth())
					.fillMaxSize(),
				verticalArrangement = Arrangement.Center,
				horizontalAlignment = Alignment.Start,
			) {
				Row(
					verticalAlignment = Alignment.Companion.CenterVertically,
					horizontalArrangement = Arrangement.spacedBy(16.dp.scaledWidth()),
				) {
					Row(
						horizontalArrangement = Arrangement.spacedBy(
							16.dp.scaledWidth(),
						),
						verticalAlignment = Alignment.Companion.CenterVertically,
						modifier = Modifier.padding(vertical = if (description == null) 10.dp.scaledHeight() else 0.dp),
					) {
						leadingIcon?.let {
							Icon(
								leadingIcon,
								leadingIcon.name,
								Modifier.size(iconSize),
								if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
							)
						}
						Column {
							Text(
								title,
								style = MaterialTheme.typography.titleMedium,
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
}
