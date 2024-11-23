package com.zaneschepke.wireguardautotunnel.data.domain

import com.zaneschepke.wireguardautotunnel.ui.theme.Theme

data class GeneralState(
	val isLocationDisclosureShown: Boolean = LOCATION_DISCLOSURE_SHOWN_DEFAULT,
	val isBatteryOptimizationDisableShown: Boolean = BATTERY_OPTIMIZATION_DISABLE_SHOWN_DEFAULT,
	val isPinLockEnabled: Boolean = PIN_LOCK_ENABLED_DEFAULT,
	val isTunnelStatsExpanded: Boolean = IS_TUNNEL_STATS_EXPANDED,
	val isLocalLogsEnabled: Boolean = IS_LOGS_ENABLED_DEFAULT,
	val locale: String? = null,
	val theme: Theme = Theme.AUTOMATIC,
) {
	companion object {
		const val LOCATION_DISCLOSURE_SHOWN_DEFAULT = false
		const val BATTERY_OPTIMIZATION_DISABLE_SHOWN_DEFAULT = false
		const val PIN_LOCK_ENABLED_DEFAULT = false
		const val IS_TUNNEL_STATS_EXPANDED = false
		const val IS_LOGS_ENABLED_DEFAULT = false
	}
}
