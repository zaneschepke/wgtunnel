package com.zaneschepke.wireguardautotunnel.data.repository

import com.zaneschepke.wireguardautotunnel.data.domain.TunnelConfig

interface AppDataRepository {
	suspend fun getPrimaryOrFirstTunnel(): TunnelConfig?

	suspend fun getStartTunnelConfig(): TunnelConfig?

	suspend fun toggleWatcherServicePause()

	val settings: SettingsRepository
	val tunnels: TunnelConfigRepository
	val appState: AppStateRepository
}
