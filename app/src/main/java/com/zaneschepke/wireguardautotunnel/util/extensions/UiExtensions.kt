package com.zaneschepke.wireguardautotunnel.util.extensions

import android.content.Context
import androidx.navigation.NavController
import com.zaneschepke.networkmonitor.AndroidNetworkMonitor
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.Route
import com.zaneschepke.wireguardautotunnel.ui.navigation.isCurrentRoute

fun NavController.goFromRoot(route: Route) {
    if (currentBackStackEntry?.isCurrentRoute(route::class) == true) return
    this.navigate(route) {
        popUpTo(Route.Main) { saveState = true }
        launchSingleTop = true
    }
}

fun AndroidNetworkMonitor.WifiDetectionMethod.asString(context: Context): String {
    return when (this) {
        AndroidNetworkMonitor.WifiDetectionMethod.DEFAULT -> context.getString(R.string._default)
        AndroidNetworkMonitor.WifiDetectionMethod.LEGACY -> context.getString(R.string.legacy)
        AndroidNetworkMonitor.WifiDetectionMethod.ROOT -> context.getString(R.string.root)
        AndroidNetworkMonitor.WifiDetectionMethod.SHIZUKU -> context.getString(R.string.shizuku)
    }
}

fun AndroidNetworkMonitor.WifiDetectionMethod.asDescriptionString(context: Context): String? {
    return when (this) {
        AndroidNetworkMonitor.WifiDetectionMethod.LEGACY ->
            context.getString(R.string.legacy_api_description)
        AndroidNetworkMonitor.WifiDetectionMethod.ROOT ->
            context.getString(R.string.use_root_shell_for_wifi)
        AndroidNetworkMonitor.WifiDetectionMethod.SHIZUKU ->
            context.getString(R.string.use_shell_via_shizuku)
        AndroidNetworkMonitor.WifiDetectionMethod.DEFAULT ->
            context.getString(R.string.use_android_recommended)
    }
}
