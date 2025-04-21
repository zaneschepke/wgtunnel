package com.zaneschepke.wireguardautotunnel.ui.navigation

import android.annotation.SuppressLint
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import com.zaneschepke.wireguardautotunnel.ui.Route
import kotlin.reflect.KClass

@SuppressLint("RestrictedApi")
fun <T : Route> NavBackStackEntry?.isCurrentRoute(cls: KClass<T>): Boolean {
    return this?.destination?.hierarchy?.any { it.hasRoute(route = cls) } == true
}

val LocalNavController =
    compositionLocalOf<NavHostController> { error("NavController was not provided") }
val LocalIsAndroidTV = staticCompositionLocalOf { false }
