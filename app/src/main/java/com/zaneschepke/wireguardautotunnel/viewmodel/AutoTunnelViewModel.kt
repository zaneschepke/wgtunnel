package com.zaneschepke.wireguardautotunnel.viewmodel

import androidx.lifecycle.viewModelScope
import com.wireguard.android.util.RootShell
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.di.AppShell
import com.zaneschepke.wireguardautotunnel.di.IoDispatcher
import com.zaneschepke.wireguardautotunnel.domain.repository.AppDataRepository
import com.zaneschepke.wireguardautotunnel.ui.common.snackbar.SnackbarController
import com.zaneschepke.wireguardautotunnel.util.StringValue
import com.zaneschepke.wireguardautotunnel.util.extensions.withData
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
	appDataRepository: AppDataRepository,
	@AppShell private val rootShell: Provider<RootShell>,
	@IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : BaseViewModel(appDataRepository) {

	fun onToggleTunnelOnWifi() = viewModelScope.launch {
		appSettings.withData {
			saveAppSettings(
				it.copy(
					isTunnelOnWifiEnabled = !it.isTunnelOnWifiEnabled,
				),
			)
		}
	}

	fun onToggleTunnelOnMobileData() = viewModelScope.launch {
		appSettings.withData {
			saveAppSettings(
				it.copy(
					isTunnelOnMobileDataEnabled = !it.isTunnelOnMobileDataEnabled,
				),
			)
		}
	}

	fun onToggleWildcards() = viewModelScope.launch {
		appSettings.withData {
			saveAppSettings(
				it.copy(
					isWildcardsEnabled = !it.isWildcardsEnabled,
				),
			)
		}
	}

	fun onDeleteTrustedSSID(ssid: String) = viewModelScope.launch {
		appSettings.withData {
			saveAppSettings(
				it.copy(
					trustedNetworkSSIDs = (it.trustedNetworkSSIDs - ssid).toMutableList(),
				),
			)
		}
	}

	fun onRootShellWifiToggle() = viewModelScope.launch {
		requestRoot().onSuccess {
			appSettings.withData {
				saveAppSettings(
					it.copy(isWifiNameByShellEnabled = !it.isWifiNameByShellEnabled),
				)
			}
		}
	}

	private suspend fun requestRoot(): Result<Unit> {
		return withContext(ioDispatcher) {
			runCatching {
				rootShell.get().start()
				SnackbarController.Companion.showMessage(StringValue.StringResource(R.string.root_accepted))
			}.onFailure {
				SnackbarController.Companion.showMessage(StringValue.StringResource(R.string.error_root_denied))
			}
		}
	}

	fun onToggleTunnelOnEthernet() = viewModelScope.launch {
		appSettings.withData {
			saveAppSettings(
				it.copy(isTunnelOnEthernetEnabled = !it.isTunnelOnEthernetEnabled),
			)
		}
	}

	fun onSaveTrustedSSID(ssid: String) = viewModelScope.launch {
		if (ssid.isEmpty()) return@launch
		val trimmed = ssid.trim()
		appSettings.withData {
			if (!it.trustedNetworkSSIDs.contains(trimmed)) {
				saveAppSettings(
					it.copy(
						trustedNetworkSSIDs = (it.trustedNetworkSSIDs + ssid).toMutableList(),
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
	}

	fun onToggleStopOnNoInternet() = viewModelScope.launch {
		appSettings.withData {
			saveAppSettings(
				it.copy(isStopOnNoInternetEnabled = !it.isStopOnNoInternetEnabled),
			)
		}
	}

	fun onToggleStopKillSwitchOnTrusted() = viewModelScope.launch {
		appSettings.withData {
			saveAppSettings(
				it.copy(isDisableKillSwitchOnTrustedEnabled = !it.isDisableKillSwitchOnTrustedEnabled),
			)
		}
	}
}
