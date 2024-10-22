package com.zaneschepke.wireguardautotunnel.ui.screens.settings.autotunnel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.data.domain.Settings
import com.zaneschepke.wireguardautotunnel.data.repository.AppDataRepository
import com.zaneschepke.wireguardautotunnel.ui.common.snackbar.SnackbarController
import com.zaneschepke.wireguardautotunnel.util.StringValue
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AutoTunnelViewModel
@Inject
constructor(
	private val appDataRepository: AppDataRepository,

) : ViewModel() {

	private val settings = appDataRepository.settings.getSettingsFlow()
		.stateIn(viewModelScope, SharingStarted.Eagerly, Settings())

	fun onToggleTunnelOnWifi() = viewModelScope.launch {
		with(settings.value) {
			appDataRepository.settings.save(
				copy(
					isTunnelOnWifiEnabled = !isTunnelOnWifiEnabled,
				),
			)
		}
	}

	fun onToggleTunnelOnMobileData() = viewModelScope.launch {
		with(settings.value) {
			appDataRepository.settings.save(
				copy(
					isTunnelOnMobileDataEnabled = !this.isTunnelOnMobileDataEnabled,
				),
			)
		}
	}

	fun onDeleteTrustedSSID(ssid: String) = viewModelScope.launch {
		with(settings.value) {
			appDataRepository.settings.save(
				copy(
					trustedNetworkSSIDs = (this.trustedNetworkSSIDs - ssid).toMutableList(),
				),
			)
		}
	}

	fun onToggleTunnelOnEthernet() = viewModelScope.launch {
		with(settings.value) {
			appDataRepository.settings.save(
				copy(
					isTunnelOnEthernetEnabled = !isTunnelOnEthernetEnabled,
				),
			)
		}
	}

	fun onSaveTrustedSSID(ssid: String) = viewModelScope.launch {
		if (ssid.isEmpty()) return@launch
		val trimmed = ssid.trim()
		with(settings.value) {
			if (!trustedNetworkSSIDs.contains(trimmed)) {
				this.trustedNetworkSSIDs.add(ssid)
				appDataRepository.settings.save(this)
			} else {
				SnackbarController.showMessage(
					StringValue.StringResource(
						R.string.error_ssid_exists,
					),
				)
			}
		}
	}

	fun onToggleRestartOnPing() = viewModelScope.launch {
		with(settings.value) {
			appDataRepository.settings.save(
				copy(
					isPingEnabled = !isPingEnabled,
				),
			)
		}
	}
}
