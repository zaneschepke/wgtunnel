package com.zaneschepke.wireguardautotunnel.ui.screens.settings.autotunnel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wireguard.android.util.RootShell
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.data.domain.Settings
import com.zaneschepke.wireguardautotunnel.data.repository.AppDataRepository
import com.zaneschepke.wireguardautotunnel.module.AppShell
import com.zaneschepke.wireguardautotunnel.module.IoDispatcher
import com.zaneschepke.wireguardautotunnel.ui.common.snackbar.SnackbarController
import com.zaneschepke.wireguardautotunnel.util.StringValue
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Provider

@HiltViewModel
class AutoTunnelViewModel
@Inject
constructor(
	private val appDataRepository: AppDataRepository,
	@AppShell private val rootShell: Provider<RootShell>,
	@IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

	fun onToggleTunnelOnWifi(settings: Settings) = viewModelScope.launch {
		with(settings) {
			appDataRepository.settings.save(
				copy(
					isTunnelOnWifiEnabled = !isTunnelOnWifiEnabled,
				),
			)
		}
	}

	fun onToggleTunnelOnMobileData(settings: Settings) = viewModelScope.launch {
		with(settings) {
			appDataRepository.settings.save(
				copy(
					isTunnelOnMobileDataEnabled = !isTunnelOnMobileDataEnabled,
				),
			)
		}
	}

	fun onToggleWildcards(settings: Settings) = viewModelScope.launch {
		with(settings) {
			appDataRepository.settings.save(
				copy(
					isWildcardsEnabled = !isWildcardsEnabled,
				),
			)
		}
	}

	fun onDeleteTrustedSSID(ssid: String, settings: Settings) = viewModelScope.launch {
		with(settings) {
			appDataRepository.settings.save(
				copy(
					trustedNetworkSSIDs = (trustedNetworkSSIDs - ssid).toMutableList(),
				),
			)
		}
	}

	fun onRootShellWifiToggle(settings: Settings) = viewModelScope.launch {
		requestRoot().onSuccess {
			with(settings) {
				appDataRepository.settings.save(
					copy(isWifiNameByShellEnabled = !isWifiNameByShellEnabled),
				)
			}
		}
	}

	private suspend fun requestRoot(): Result<Unit> {
		return withContext(ioDispatcher) {
			kotlin.runCatching {
				rootShell.get().start()
				SnackbarController.showMessage(StringValue.StringResource(R.string.root_accepted))
			}.onFailure {
				SnackbarController.showMessage(StringValue.StringResource(R.string.error_root_denied))
			}
		}
	}

	fun onToggleTunnelOnEthernet(settings: Settings) = viewModelScope.launch {
		with(settings) {
			appDataRepository.settings.save(
				copy(
					isTunnelOnEthernetEnabled = !isTunnelOnEthernetEnabled,
				),
			)
		}
	}

	fun onSaveTrustedSSID(ssid: String, settings: Settings) = viewModelScope.launch {
		if (ssid.isEmpty()) return@launch
		val trimmed = ssid.trim()
		with(settings) {
			if (!trustedNetworkSSIDs.contains(trimmed)) {
				appDataRepository.settings.save(
					settings.copy(
						trustedNetworkSSIDs = (trustedNetworkSSIDs + ssid).toMutableList(),
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
	}

	fun onToggleStopOnNoInternet(settings: Settings) = viewModelScope.launch {
		with(settings) {
			appDataRepository.settings.save(
				copy(isStopOnNoInternetEnabled = !isStopOnNoInternetEnabled),
			)
		}
	}

	fun onToggleStopKillSwitchOnTrusted(settings: Settings) = viewModelScope.launch {
		with(settings) {
			appDataRepository.settings.save(
				copy(isDisableKillSwitchOnTrustedEnabled = !isDisableKillSwitchOnTrustedEnabled),
			)
		}
	}
}
