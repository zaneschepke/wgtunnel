package com.zaneschepke.wireguardautotunnel.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.QuestionMark
import androidx.compose.material.icons.rounded.Settings
import com.zaneschepke.wireguardautotunnel.ui.common.navigation.BottomNavItem

sealed class Screen(val route : String) {
    data object Main: Screen("main") {
        val navItem = BottomNavItem(
        name = "Tunnels",
        route = route,
        icon = Icons.Rounded.Home
        )
    }
    data object Settings: Screen("settings") {
        val navItem = BottomNavItem(
        name = "Settings",
        route = route,
        icon = Icons.Rounded.Settings
        )
    }
    data object Support: Screen("support") {
        val navItem = BottomNavItem(
        name = "Support",
        route = route,
        icon = Icons.Rounded.QuestionMark
        )
    }
    data object Config : Screen("config")

}