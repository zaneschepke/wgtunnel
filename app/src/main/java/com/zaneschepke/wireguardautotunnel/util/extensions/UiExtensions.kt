package com.zaneschepke.wireguardautotunnel.util.extensions

import androidx.navigation.NavController
import com.zaneschepke.wireguardautotunnel.ui.Route
import com.zaneschepke.wireguardautotunnel.ui.common.navigation.isCurrentRoute

fun NavController.goFromRoot(route: Route) {
	if (currentBackStackEntry?.isCurrentRoute(route::class) == true) return
	this.navigate(route) {
		popUpTo(Route.Main) {
			saveState = true
		}
		launchSingleTop = true
	}
}
