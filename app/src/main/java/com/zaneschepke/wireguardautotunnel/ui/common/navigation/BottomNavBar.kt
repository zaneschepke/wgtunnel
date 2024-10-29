package com.zaneschepke.wireguardautotunnel.ui.common.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import timber.log.Timber

@Composable
fun BottomNavBar(navController: NavController, bottomNavItems: List<BottomNavItem>) {
	var showBottomBar by rememberSaveable { mutableStateOf(true) }
	val navBackStackEntry by navController.currentBackStackEntryAsState()

	showBottomBar = bottomNavItems.any {
		navBackStackEntry?.isCurrentRoute(it.route::class) == true
	}

	if (showBottomBar) {

		NavigationBar(
			containerColor = MaterialTheme.colorScheme.surface,
		) {
			bottomNavItems.forEachIndexed { index, item ->
				val selected = navBackStackEntry.isCurrentRoute(item.route::class)
				NavigationBarItem(
					selected = selected,
					onClick = {
						if (selected) return@NavigationBarItem
						navController.navigate(item.route) {
							// Pop up to the start destination of the graph to
							// avoid building up a large stack of destinations
							// on the back stack as users select items
							popUpTo(navController.graph.findStartDestination().id) {
								saveState = true
							}
							// Avoid multiple copies of the same destination when
							// reselecting the same item
							launchSingleTop = true
						}
					},
					label = {
						Text(
							text = item.name,
							style = MaterialTheme.typography.labelMedium,
						)
					},
					icon = {
						Icon(
							imageVector = item.icon,
							contentDescription = "${item.name} Icon",
						)
					},
				)
			}
		}
	}
}
