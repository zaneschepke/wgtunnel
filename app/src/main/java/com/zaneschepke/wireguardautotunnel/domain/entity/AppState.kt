package com.zaneschepke.wireguardautotunnel.domain.entity

import com.zaneschepke.wireguardautotunnel.ui.theme.Theme

data class AppState(
	val isLocationDisclosureShown: Boolean,
	val isBatteryOptimizationDisableShown: Boolean,
	val isPinLockEnabled: Boolean,
	val isTunnelStatsExpanded: Boolean,
	val isLocalLogsEnabled: Boolean,
	val locale: String?,
	val theme: Theme,
)
