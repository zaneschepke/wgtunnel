package com.zaneschepke.wireguardautotunnel.ui.common.navigation

import android.annotation.SuppressLint
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import com.zaneschepke.wireguardautotunnel.ui.Route

@SuppressLint("RestrictedApi")
fun NavBackStackEntry?.isCurrentRoute(route: Route): Boolean {
	return this?.destination?.hierarchy?.any {
		it.hasRoute(route = route::class)
	} == true
}
