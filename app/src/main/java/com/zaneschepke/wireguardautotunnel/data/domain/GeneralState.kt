package com.zaneschepke.wireguardautotunnel.data.domain

data class GeneralState(
	val isLocationDisclosureShown: Boolean = LOCATION_DISCLOSURE_SHOWN_DEFAULT,
	val isBatteryOptimizationDisableShown: Boolean = BATTERY_OPTIMIZATION_DISABLE_SHOWN_DEFAULT,
	val isPinLockEnabled: Boolean = PIN_LOCK_ENABLED_DEFAULT,
	val isTunnelStatsExpanded: Boolean = IS_TUNNEL_STATS_EXPANDED,
) {
	companion object {
		const val LOCATION_DISCLOSURE_SHOWN_DEFAULT = false
		const val BATTERY_OPTIMIZATION_DISABLE_SHOWN_DEFAULT = false
		const val PIN_LOCK_ENABLED_DEFAULT = false
		const val IS_TUNNEL_STATS_EXPANDED = false
	}
}
