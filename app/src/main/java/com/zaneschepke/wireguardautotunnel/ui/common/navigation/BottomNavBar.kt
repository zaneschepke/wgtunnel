package com.zaneschepke.wireguardautotunnel.ui.common.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState

@Composable
fun BottomNavBar(navController: NavController, bottomNavItems: List<BottomNavItem>) {
	var showBottomBar by rememberSaveable { mutableStateOf(true) }
	val navBackStackEntry by navController.currentBackStackEntryAsState()

	showBottomBar = bottomNavItems.firstOrNull { navBackStackEntry?.destination?.route?.contains(it.route) == true } != null

	if(showBottomBar) {
		NavigationBar(
			containerColor = MaterialTheme.colorScheme.surface,
		) {
			bottomNavItems.forEach { item ->
				val selected = navBackStackEntry?.destination?.route?.contains(item.route) == true

				NavigationBarItem(
					selected = selected,
					onClick = {
						if(navBackStackEntry?.destination?.route == item.route) return@NavigationBarItem
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
							fontWeight = FontWeight.SemiBold,
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
