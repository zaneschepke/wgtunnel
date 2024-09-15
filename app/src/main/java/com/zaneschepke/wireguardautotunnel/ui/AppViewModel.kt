package com.zaneschepke.wireguardautotunnel.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import com.zaneschepke.wireguardautotunnel.data.repository.AppDataRepository
import com.zaneschepke.wireguardautotunnel.service.tunnel.TunnelService
import com.zaneschepke.wireguardautotunnel.util.Constants
import com.zaneschepke.wireguardautotunnel.util.extensions.TunnelConfigs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import xyz.teamgravity.pin_lock_compose.PinManager
import javax.inject.Inject

@HiltViewModel
class AppViewModel
@Inject
constructor(
	private val appDataRepository: AppDataRepository,
	private val tunnelService: TunnelService,
	val navHostController: NavHostController,
) : ViewModel() {

	private val _appUiState = MutableStateFlow(AppUiState())

	val uiState =
		combine(
			appDataRepository.settings.getSettingsFlow(),
			appDataRepository.tunnels.getTunnelConfigsFlow(),
			tunnelService.vpnState,
			appDataRepository.appState.generalStateFlow,
		) { settings, tunnels, tunnelState, generalState ->
			AppUiState(
				settings,
				tunnels,
				tunnelState,
				generalState,
			)
		}
			.stateIn(
				viewModelScope,
				SharingStarted.WhileSubscribed(Constants.SUBSCRIPTION_TIMEOUT),
				_appUiState.value,
			)

	fun setTunnels(tunnels: TunnelConfigs) = viewModelScope.launch {
		_appUiState.emit(
			_appUiState.value.copy(
				tunnels = tunnels,
			),
		)
	}

	fun onPinLockDisabled() = viewModelScope.launch {
		PinManager.clearPin()
		appDataRepository.appState.setPinLockEnabled(false)
	}

	fun onPinLockEnabled() = viewModelScope.launch {
		appDataRepository.appState.setPinLockEnabled(true)
	}
}
