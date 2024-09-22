package com.zaneschepke.wireguardautotunnel.ui.common.navigation

import androidx.compose.ui.graphics.vector.ImageVector
import com.zaneschepke.wireguardautotunnel.ui.Route

data class BottomNavItem(
	val name: String,
	val route: Route,
	val icon: ImageVector,
)
