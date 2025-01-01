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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
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
					isTunnelOnMobileDataEnabled = !isTunnelOnMobileDataEnabled,
				),
			)
		}
	}

	fun onToggleWildcards() = viewModelScope.launch {
		with(settings.value) {
			appDataRepository.settings.save(
				copy(
					isWildcardsEnabled = !isWildcardsEnabled,
				),
			)
		}
	}

	fun onDeleteTrustedSSID(ssid: String) = viewModelScope.launch {
		with(settings.value) {
			appDataRepository.settings.save(
				copy(
					trustedNetworkSSIDs = (trustedNetworkSSIDs - ssid).toMutableList(),
				),
			)
		}
	}

	fun onRootShellWifiToggle() = viewModelScope.launch {
		requestRoot().onSuccess {
			with(settings.value) {
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

	fun onToggleStopOnNoInternet() = viewModelScope.launch {
		with(settings.value) {
			appDataRepository.settings.save(
				copy(isStopOnNoInternetEnabled = !isStopOnNoInternetEnabled),
			)
		}
	}
}
