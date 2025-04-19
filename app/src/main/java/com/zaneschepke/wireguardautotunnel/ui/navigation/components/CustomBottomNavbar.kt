package com.zaneschepke.wireguardautotunnel.ui.navigation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.ui.Route
import com.zaneschepke.wireguardautotunnel.ui.navigation.BottomNavItem
import com.zaneschepke.wireguardautotunnel.ui.state.NavBarState

@Composable
fun CustomBottomNavbar(tabs: List<BottomNavItem>, navBarState: NavBarState) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var isChildRoute by remember { mutableStateOf(false) }

    LaunchedEffect(tabs) {}
    when (navBarState.route) {
        Route.Main -> {
            selectedTabIndex = 0
            isChildRoute = false
        }
        Route.AutoTunnel -> {
            selectedTabIndex = 1
            isChildRoute = false
        }
        Route.Settings -> {
            selectedTabIndex = 2
            isChildRoute = false
        }
        Route.Support -> {
            selectedTabIndex = 3
            isChildRoute = false
        }
        else -> isChildRoute = true
    }

    val systemBars = WindowInsets.systemBars
    val bottomPadding = with(LocalDensity.current) { systemBars.getBottom(this).toDp() }
    val navHeight = 64.dp + bottomPadding

    Box(modifier = Modifier.fillMaxWidth().height(navHeight).background(Color.Transparent)) {
        BottomBarTabs(
            tabs = tabs,
            selectedTabIndex = selectedTabIndex,
            isChildRoute = isChildRoute,
            onTabSelected = { selectedTabIndex = tabs.indexOf(it) },
        )

        val animatedSelectedTabIndex by
            animateFloatAsState(
                targetValue = selectedTabIndex.toFloat(),
                label = "animatedSelectedTabIndex",
                animationSpec =
                    spring(
                        stiffness = Spring.StiffnessLow,
                        dampingRatio = Spring.DampingRatioLowBouncy,
                    ),
            )

        val animatedColor by
            animateColorAsState(
                targetValue = MaterialTheme.colorScheme.primary,
                label = "animatedColor",
                animationSpec = spring(stiffness = Spring.StiffnessLow),
            )

        Canvas(modifier = Modifier.fillMaxWidth().height(navHeight)) {
            val path =
                Path().apply { addRoundRect(RoundRect(size.toRect(), CornerRadius(size.height))) }
            val length = PathMeasure().apply { setPath(path, false) }.length

            val tabWidth = size.width / tabs.size
            drawPath(
                path,
                brush =
                    Brush.horizontalGradient(
                        colors =
                            listOf(
                                animatedColor.copy(alpha = 0f),
                                animatedColor.copy(alpha = 1f),
                                animatedColor.copy(alpha = 1f),
                                animatedColor.copy(alpha = 0f),
                            ),
                        startX = tabWidth * animatedSelectedTabIndex,
                        endX = tabWidth * (animatedSelectedTabIndex + 1),
                    ),
                style =
                    Stroke(
                        width = 4f,
                        pathEffect =
                            PathEffect.dashPathEffect(intervals = floatArrayOf(length / 2, length)),
                    ),
            )
        }
    }
}
