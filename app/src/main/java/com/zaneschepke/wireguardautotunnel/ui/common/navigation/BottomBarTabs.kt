package com.zaneschepke.wireguardautotunnel.ui.common.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.ui.theme.SilverTree
import com.zaneschepke.wireguardautotunnel.util.extensions.isRunningOnTv

@Composable
fun BottomBarTabs(tabs: List<BottomNavItem>, selectedTabIndex: Int, isChildRoute: Boolean, onTabSelected: (BottomNavItem) -> Unit) {
	val context = LocalContext.current
	val isRunningOnTv = remember { context.isRunningOnTv() }

	Row(
		modifier = Modifier
			.fillMaxWidth()
			.height(64.dp)
			.padding(horizontal = 8.dp)
			.padding(top = 12.dp),
		horizontalArrangement = Arrangement.SpaceEvenly,
		verticalAlignment = Alignment.CenterVertically,
	) {
		tabs.forEachIndexed { index, tab ->
			Column(
				modifier = Modifier
					.weight(1f)
					.fillMaxHeight()
					.background(Color.Transparent)
					.then(
						if (isRunningOnTv) {
							Modifier.clickable {
								if (index == selectedTabIndex && !isChildRoute) return@clickable
								tab.onClick.invoke()
								onTabSelected(tab)
							}
						} else {
							Modifier
						},
					)
					.pointerInput(Unit) {
						detectTapGestures {
							if (index == selectedTabIndex && !isChildRoute) return@detectTapGestures
							tab.onClick.invoke()
							onTabSelected(tab)
						}
					},
				horizontalAlignment = Alignment.CenterHorizontally,
				verticalArrangement = Arrangement.Center,
			) {
				val animatedColor by animateColorAsState(
					targetValue = MaterialTheme.colorScheme.primary,
					animationSpec = spring(stiffness = Spring.StiffnessLow),
					label = "animatedColor",
				)
				val color = if (selectedTabIndex == index) animatedColor else MaterialTheme.colorScheme.onSurface

				if (tab.active) {
					BadgedBox(
						badge = {
							Badge(
								modifier = Modifier
									.offset(x = 8.dp, y = ((-8).dp))
									.size(6.dp),
								containerColor = SilverTree,
							)
						},
					) {
						Icon(
							imageVector = tab.icon,
							contentDescription = tab.name,
							tint = color,
							modifier = Modifier.size(24.dp),
						)
					}
				} else {
					Icon(
						imageVector = tab.icon,
						contentDescription = tab.name,
						tint = color,
						modifier = Modifier.size(24.dp),
					)
				}
			}
		}
	}
}
