package com.zaneschepke.wireguardautotunnel.data.model

import com.zaneschepke.wireguardautotunnel.domain.entity.AppState
import com.zaneschepke.wireguardautotunnel.ui.theme.Theme

data class GeneralState(
    val isLocationDisclosureShown: Boolean = LOCATION_DISCLOSURE_SHOWN_DEFAULT,
    val isBatteryOptimizationDisableShown: Boolean = BATTERY_OPTIMIZATION_DISABLE_SHOWN_DEFAULT,
    val isPinLockEnabled: Boolean = PIN_LOCK_ENABLED_DEFAULT,
    val isTunnelStatsExpanded: Boolean = IS_TUNNEL_STATS_EXPANDED,
    val isLocalLogsEnabled: Boolean = IS_LOGS_ENABLED_DEFAULT,
    val isRemoteControlEnabled: Boolean = IS_REMOTE_CONTROL_ENABLED,
    val remoteKey: String? = null,
    val locale: String? = null,
    val theme: Theme = Theme.AUTOMATIC,
) {

    fun toAppState(): AppState =
        AppState(
            isLocationDisclosureShown,
            isBatteryOptimizationDisableShown,
            isPinLockEnabled,
            isTunnelStatsExpanded,
            isLocationDisclosureShown,
            isRemoteControlEnabled,
            remoteKey,
            locale,
            theme,
        )

    companion object {
        fun from(appState: AppState): GeneralState {
            return with(appState) {
                GeneralState(
                    isLocationDisclosureShown,
                    isBatteryOptimizationDisableShown,
                    isPinLockEnabled,
                    isTunnelStatsExpanded,
                    isLocalLogsEnabled,
                    isRemoteControlEnabled,
                    remoteKey,
                    locale,
                    theme,
                )
            }
        }

        const val LOCATION_DISCLOSURE_SHOWN_DEFAULT = false
        const val BATTERY_OPTIMIZATION_DISABLE_SHOWN_DEFAULT = false
        const val PIN_LOCK_ENABLED_DEFAULT = false
        const val IS_TUNNEL_STATS_EXPANDED = false
        const val IS_LOGS_ENABLED_DEFAULT = false
        const val IS_REMOTE_CONTROL_ENABLED = false
    }
}
