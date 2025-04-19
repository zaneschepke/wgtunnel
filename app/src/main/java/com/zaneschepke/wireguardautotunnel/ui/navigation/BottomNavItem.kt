package com.zaneschepke.wireguardautotunnel.ui.navigation

import androidx.compose.ui.graphics.vector.ImageVector
import com.zaneschepke.wireguardautotunnel.ui.Route

data class BottomNavItem(
    val name: String,
    val route: Route,
    val icon: ImageVector,
    val onClick: () -> Unit,
    val active: Boolean = false,
)
