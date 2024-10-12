package com.zaneschepke.wireguardautotunnel.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zaneschepke.wireguardautotunnel.WireGuardAutoTunnel
import com.zaneschepke.wireguardautotunnel.data.repository.AppDataRepository
import com.zaneschepke.wireguardautotunnel.module.IoDispatcher
import com.zaneschepke.wireguardautotunnel.service.foreground.ServiceManager
import com.zaneschepke.wireguardautotunnel.service.tunnel.TunnelService
import com.zaneschepke.wireguardautotunnel.service.tunnel.TunnelState
import com.zaneschepke.wireguardautotunnel.util.Constants
import com.zaneschepke.wireguardautotunnel.util.extensions.TunnelConfigs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import xyz.teamgravity.pin_lock_compose.PinManager
import javax.inject.Inject
import javax.inject.Provider

@HiltViewModel
class AppViewModel
@Inject
constructor(
	private val appDataRepository: AppDataRepository,
	private val tunnelService: Provider<TunnelService>,
	@IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

	private val _appUiState = MutableStateFlow(AppUiState())

	val uiState =
		combine(
			appDataRepository.settings.getSettingsFlow(),
			appDataRepository.tunnels.getTunnelConfigsFlow(),
			tunnelService.get().vpnState,
			appDataRepository.appState.generalStateFlow,
		) { settings, tunnels, tunnelState, generalState ->
			AppUiState(
				settings,
				tunnels,
				tunnelState,
				generalState,
			)
		}.stateIn(
			viewModelScope + ioDispatcher,
			SharingStarted.WhileSubscribed(Constants.SUBSCRIPTION_TIMEOUT),
			_appUiState.value,
		)

	private val _isAppReady = MutableStateFlow<Boolean>(false)
	val isAppReady = _isAppReady.asStateFlow()

	init {
	    viewModelScope.launch {
			initPin()
			initAutoTunnel()
			initTunnel()
			appReadyCheck()
		}
	}

	private suspend fun appReadyCheck() {
		val tunnelCount = appDataRepository.tunnels.count()
		uiState.takeWhile { it.tunnels.size != tunnelCount }.onCompletion {
			_isAppReady.emit(true)
		}.collect()
	}

	private suspend fun initTunnel() {
		if (tunnelService.get().getState() == TunnelState.UP) tunnelService.get().startStatsJob()
		val activeTunnels = appDataRepository.tunnels.getActive()
		if (activeTunnels.isNotEmpty() &&
			tunnelService.get().getState() == TunnelState.DOWN
		) {
			tunnelService.get().startTunnel(activeTunnels.first())
		}
	}

	private suspend fun initPin() {
		val isPinEnabled = appDataRepository.appState.isPinLockEnabled()
		if(isPinEnabled) PinManager.initialize(WireGuardAutoTunnel.instance)
	}

	private suspend fun initAutoTunnel() {
		val settings = appDataRepository.settings.getSettings()
		if (settings.isAutoTunnelEnabled) ServiceManager.startWatcherService(WireGuardAutoTunnel.instance)
	}

	fun setTunnels(tunnels: TunnelConfigs) = viewModelScope.launch(ioDispatcher) {
		_appUiState.emit(
			_appUiState.value.copy(
				tunnels = tunnels,
			),
		)
	}

	fun onPinLockDisabled() = viewModelScope.launch(ioDispatcher) {
		PinManager.clearPin()
		appDataRepository.appState.setPinLockEnabled(false)
	}

	fun onPinLockEnabled() = viewModelScope.launch {
		appDataRepository.appState.setPinLockEnabled(true)
	}
}
