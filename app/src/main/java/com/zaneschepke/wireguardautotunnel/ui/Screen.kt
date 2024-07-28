package com.zaneschepke.wireguardautotunnel.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.QuestionMark
import androidx.compose.material.icons.rounded.Settings
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.WireGuardAutoTunnel
import com.zaneschepke.wireguardautotunnel.ui.common.navigation.BottomNavItem

sealed class Screen(val route: String) {
	data object Main : Screen("main") {
		val navItem =
			BottomNavItem(
				name = WireGuardAutoTunnel.instance.getString(R.string.tunnels),
				route = route,
				icon = Icons.Rounded.Home,
			)
	}

	data object Settings : Screen("settings") {
		val navItem =
			BottomNavItem(
				name = WireGuardAutoTunnel.instance.getString(R.string.settings),
				route = route,
				icon = Icons.Rounded.Settings,
			)
	}

	data object Support : Screen("support") {
		val navItem =
			BottomNavItem(
				name = WireGuardAutoTunnel.instance.getString(R.string.support),
				route = route,
				icon = Icons.Rounded.QuestionMark,
			)

		data object Logs : Screen("support/logs")
	}

	data object Config : Screen("config")

	data object Lock : Screen("lock")

	data object Option : Screen("option")
}
