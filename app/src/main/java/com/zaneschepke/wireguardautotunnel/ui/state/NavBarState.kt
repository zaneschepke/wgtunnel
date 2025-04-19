package com.zaneschepke.wireguardautotunnel.ui.state

import androidx.compose.runtime.Composable
import com.zaneschepke.wireguardautotunnel.ui.Route

data class NavBarState(
    val showTop: Boolean = true,
    val showBottom: Boolean = true,
    val topTitle: @Composable (() -> Unit)? = null,
    val topTrailing: @Composable (() -> Unit)? = null,
    val route: Route? = null,
)
