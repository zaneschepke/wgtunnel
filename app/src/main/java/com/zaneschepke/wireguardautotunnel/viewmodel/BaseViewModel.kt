package com.zaneschepke.wireguardautotunnel.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zaneschepke.wireguardautotunnel.domain.entity.AppSettings
import com.zaneschepke.wireguardautotunnel.domain.entity.TunnelConf
import com.zaneschepke.wireguardautotunnel.domain.repository.AppDataRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
open class BaseViewModel @Inject constructor(
	protected val appDataRepository: AppDataRepository,
) : ViewModel() {

	val appSettings: StateFlow<AppSettings?> = appDataRepository.settings.flow.stateIn(
		scope = viewModelScope,
		started = SharingStarted.Companion.WhileSubscribed(5000),
		initialValue = null,
	)

	val tunnels: StateFlow<List<TunnelConf>?> = appDataRepository.tunnels.flow.stateIn(
		scope = viewModelScope,
		started = SharingStarted.Companion.WhileSubscribed(5000),
		initialValue = null,
	)

	fun saveAppSettings(appSettings: AppSettings) = viewModelScope.launch {
		appDataRepository.settings.save(appSettings)
	}

	fun saveTunnel(tunnelConf: TunnelConf) = viewModelScope.launch {
		appDataRepository.tunnels.save(tunnelConf)
	}
}
