package com.zaneschepke.wireguardautotunnel.ui.screens.main.splittunnel

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.ui.common.animation.ShimmerEffect
import com.zaneschepke.wireguardautotunnel.ui.theme.iconSize

@Composable
fun SplitTunnelSkeleton() {
	val shimmerBrush = ShimmerEffect()

	Column(
		verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
		horizontalAlignment = Alignment.CenterHorizontally,
		modifier = Modifier
			.fillMaxWidth()
			.padding(top = 24.dp),
	) {
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.padding(horizontal = 24.dp)
				.height(45.dp),
			horizontalArrangement = Arrangement.spacedBy(8.dp),
		) {
			repeat(3) {
				Box(
					modifier = Modifier
						.weight(1f)
						.height(45.dp)
						.clip(RoundedCornerShape(8.dp))
						.background(shimmerBrush),
				)
			}
		}

		Row(
			modifier = Modifier
				.fillMaxWidth()
				.padding(horizontal = 24.dp)
				.height(45.dp),
			horizontalArrangement = Arrangement.spacedBy(8.dp),
		) {
			Box(
				modifier = Modifier
					.height(45.dp)
					.fillMaxWidth()
					.clip(RoundedCornerShape(8.dp))
					.background(shimmerBrush),
			)
		}

		LazyColumn(
			horizontalAlignment = Alignment.CenterHorizontally,
			verticalArrangement = Arrangement.Top,
			contentPadding = PaddingValues(top = 10.dp),
			modifier = Modifier.fillMaxWidth(),
		) {
			items(20) {
				Row(
					modifier = Modifier
						.fillMaxWidth()
						.padding(horizontal = 24.dp, vertical = 8.dp),
					verticalAlignment = Alignment.CenterVertically,
				) {
					Box(
						modifier = Modifier
							.size(iconSize)
							.clip(CircleShape)
							.background(shimmerBrush),
					)
					Spacer(modifier = Modifier.width(16.dp))
					Box(
						modifier = Modifier
							.height(20.dp)
							.weight(1f)
							.clip(RoundedCornerShape(4.dp))
							.background(shimmerBrush),
					)
					Spacer(modifier = Modifier.width(16.dp))
					Box(
						modifier = Modifier
							.size(24.dp)
							.clip(CircleShape)
							.background(shimmerBrush),
					)
				}
			}
		}
	}
}
