package com.zaneschepke.wireguardautotunnel.data.repository

import com.zaneschepke.wireguardautotunnel.data.domain.TunnelConfig
import javax.inject.Inject

class AppDataRoomRepository
@Inject
constructor(
	override val settings: SettingsRepository,
	override val tunnels: TunnelConfigRepository,
	override val appState: AppStateRepository,
) : AppDataRepository {
	override suspend fun getPrimaryOrFirstTunnel(): TunnelConfig? {
		return tunnels.findPrimary().firstOrNull() ?: tunnels.getAll().firstOrNull()
	}

	override suspend fun getStartTunnelConfig(): TunnelConfig? {
		return if (appState.isTunnelRunningFromManualStart()) {
			appState.getActiveTunnelId()?.let {
				tunnels.getById(it)
			}
		} else {
			null
		}
	}

	override suspend fun toggleWatcherServicePause() {
		val settings = settings.getSettings()
		if (settings.isAutoTunnelEnabled) {
			val pauseAutoTunnel = !settings.isAutoTunnelPaused
			this.settings.save(
				settings.copy(
					isAutoTunnelPaused = pauseAutoTunnel,
				),
			)
		}
	}
}
