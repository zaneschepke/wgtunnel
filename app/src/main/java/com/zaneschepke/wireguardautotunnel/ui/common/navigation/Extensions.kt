package com.zaneschepke.wireguardautotunnel.ui.common.navigation

import android.annotation.SuppressLint
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import com.zaneschepke.wireguardautotunnel.ui.Route
import kotlin.reflect.KClass

@SuppressLint("RestrictedApi")
fun <T : Route> NavBackStackEntry?.isCurrentRoute(cls: KClass<T>): Boolean {
    return this?.destination?.hierarchy?.any { it.hasRoute(route = cls) } == true
}
