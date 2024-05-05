package com.zaneschepke.wireguardautotunnel.data.domain

data class GeneralState(
    val locationDisclosureShown: Boolean = LOCATION_DISCLOSURE_SHOWN_DEFAULT,
    val batteryOptimizationDisableShown: Boolean = BATTERY_OPTIMIZATION_DISABLE_SHOWN_DEFAULT,
    val tunnelRunningFromManualStart: Boolean = TUNNELING_RUNNING_FROM_MANUAL_START_DEFAULT,
    val activeTunnelId: Int? = null
) {
    companion object {
        const val LOCATION_DISCLOSURE_SHOWN_DEFAULT = false
        const val BATTERY_OPTIMIZATION_DISABLE_SHOWN_DEFAULT = false
        const val TUNNELING_RUNNING_FROM_MANUAL_START_DEFAULT = false
    }
}
