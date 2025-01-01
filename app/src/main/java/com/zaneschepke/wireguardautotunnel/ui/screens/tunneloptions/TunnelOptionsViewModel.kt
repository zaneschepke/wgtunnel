package com.zaneschepke.wireguardautotunnel.ui.screens.tunneloptions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zaneschepke.wireguardautotunnel.data.domain.TunnelConfig
import com.zaneschepke.wireguardautotunnel.data.repository.AppDataRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TunnelOptionsViewModel
@Inject
constructor(
	private val appDataRepository: AppDataRepository,
) : ViewModel() {
	fun onToggleRestartOnPing(tunnelConfig: TunnelConfig) = viewModelScope.launch {
		appDataRepository.tunnels.save(
			tunnelConfig.copy(
				isPingEnabled = !tunnelConfig.isPingEnabled,
			),
		)
	}

	fun onTogglePrimaryTunnel(tunnelConfig: TunnelConfig) = viewModelScope.launch {
		appDataRepository.tunnels.updatePrimaryTunnel(
			when (tunnelConfig.isPrimaryTunnel) {
				true -> null
				false -> tunnelConfig
			},
		)
	}

	fun saveTunnelChanges(tunnelConfig: TunnelConfig) = viewModelScope.launch {
		appDataRepository.tunnels.save(tunnelConfig)
	}
}
