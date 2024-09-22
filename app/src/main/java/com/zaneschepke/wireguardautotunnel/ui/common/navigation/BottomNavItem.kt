package com.zaneschepke.wireguardautotunnel.ui.common.navigation

import androidx.compose.ui.graphics.vector.ImageVector
import com.zaneschepke.wireguardautotunnel.ui.Screens

data class BottomNavItem(
	val name: String,
	val route: Screens,
	val icon: ImageVector,
)
