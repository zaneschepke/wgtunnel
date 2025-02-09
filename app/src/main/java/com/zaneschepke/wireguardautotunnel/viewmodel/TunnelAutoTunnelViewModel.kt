package com.zaneschepke.wireguardautotunnel.viewmodel

import androidx.lifecycle.viewModelScope
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.domain.entity.TunnelConf
import com.zaneschepke.wireguardautotunnel.domain.repository.AppDataRepository
import com.zaneschepke.wireguardautotunnel.ui.common.snackbar.SnackbarController
import com.zaneschepke.wireguardautotunnel.util.StringValue
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TunnelAutoTunnelViewModel
@Inject
constructor(
	appDataRepository: AppDataRepository,
) : BaseViewModel(appDataRepository) {

	fun onDeleteRunSSID(ssid: String, tunnelConfig: TunnelConf) = saveTunnel(
		tunnelConfig.copy(
			tunnelNetworks = (tunnelConfig.tunnelNetworks - ssid).toMutableList(),
		),
	)

	fun onSaveRunSSID(ssid: String, tunnelConf: TunnelConf) = viewModelScope.launch {
		if (ssid.isBlank()) return@launch
		val trimmed = ssid.trim()
		val tunnelsWithName = appDataRepository.tunnels.findByTunnelNetworksName(trimmed)

		if (!tunnelConf.tunnelNetworks.contains(trimmed) &&
			tunnelsWithName.isEmpty()
		) {
			saveTunnel(
				tunnelConf.copy(
					tunnelNetworks = (tunnelConf.tunnelNetworks + ssid).toMutableList(),
				),
			)
		} else {
			SnackbarController.Companion.showMessage(
				StringValue.StringResource(
					R.string.error_ssid_exists,
				),
			)
		}
	}

	fun onToggleIsMobileDataTunnel(tunnelConf: TunnelConf) = viewModelScope.launch {
		if (tunnelConf.isMobileDataTunnel) {
			appDataRepository.tunnels.updateMobileDataTunnel(null)
		} else {
			appDataRepository.tunnels.updateMobileDataTunnel(tunnelConf)
		}
	}

	fun onToggleIsEthernetTunnel(tunnelConf: TunnelConf) = viewModelScope.launch {
		if (tunnelConf.isEthernetTunnel) {
			appDataRepository.tunnels.updateEthernetTunnel(null)
		} else {
			appDataRepository.tunnels.updateEthernetTunnel(tunnelConf)
		}
	}
}
