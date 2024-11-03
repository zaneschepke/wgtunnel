package com.zaneschepke.wireguardautotunnel.ui.screens.options

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.data.domain.TunnelConfig
import com.zaneschepke.wireguardautotunnel.data.repository.AppDataRepository
import com.zaneschepke.wireguardautotunnel.ui.common.snackbar.SnackbarController
import com.zaneschepke.wireguardautotunnel.util.StringValue
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OptionsViewModel
@Inject
constructor(
	private val appDataRepository: AppDataRepository,
) : ViewModel() {

	fun onDeleteRunSSID(ssid: String, tunnelConfig: TunnelConfig) = viewModelScope.launch {
		appDataRepository.tunnels.save(
			tunnelConfig =
			tunnelConfig.copy(
				tunnelNetworks = (tunnelConfig.tunnelNetworks - ssid).toMutableList(),
			),
		)
	}

	fun saveTunnelChanges(tunnelConfig: TunnelConfig) = viewModelScope.launch {
		appDataRepository.tunnels.save(tunnelConfig)
	}

	fun onSaveRunSSID(ssid: String, tunnelConfig: TunnelConfig) = viewModelScope.launch {
		if (ssid.isBlank()) return@launch
		val trimmed = ssid.trim()
		val tunnelsWithName = appDataRepository.tunnels.findByTunnelNetworksName(trimmed)

		if (!tunnelConfig.tunnelNetworks.contains(trimmed) &&
			tunnelsWithName.isEmpty()
		) {
			saveTunnelChanges(
				tunnelConfig.copy(
					tunnelNetworks = (tunnelConfig.tunnelNetworks + ssid).toMutableList(),
				),
			)
		} else {
			SnackbarController.showMessage(
				StringValue.StringResource(
					R.string.error_ssid_exists,
				),
			)
		}
	}

	fun onToggleIsMobileDataTunnel(tunnelConfig: TunnelConfig) = viewModelScope.launch {
		if (tunnelConfig.isMobileDataTunnel) {
			appDataRepository.tunnels.updateMobileDataTunnel(null)
		} else {
			appDataRepository.tunnels.updateMobileDataTunnel(tunnelConfig)
		}
	}

	fun onTogglePrimaryTunnel(tunnelConfig: TunnelConfig) = viewModelScope.launch {
		appDataRepository.tunnels.updatePrimaryTunnel(
			when (tunnelConfig.isPrimaryTunnel) {
				true -> null
				false -> tunnelConfig
			},
		)
	}

	fun onToggleRestartOnPing(tunnelConfig: TunnelConfig) = viewModelScope.launch {
		appDataRepository.tunnels.save(
			tunnelConfig.copy(
				isPingEnabled = !tunnelConfig.isPingEnabled,
			),
		)
	}
}
