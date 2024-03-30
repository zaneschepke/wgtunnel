package com.zaneschepke.wireguardautotunnel.ui.screens.options

import androidx.compose.ui.util.fastFirstOrNull
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zaneschepke.wireguardautotunnel.WireGuardAutoTunnel
import com.zaneschepke.wireguardautotunnel.data.model.TunnelConfig
import com.zaneschepke.wireguardautotunnel.data.repository.AppDataRepository
import com.zaneschepke.wireguardautotunnel.util.Constants
import com.zaneschepke.wireguardautotunnel.util.Event
import com.zaneschepke.wireguardautotunnel.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class OptionsViewModel @Inject
constructor(
    private val appDataRepository: AppDataRepository
) : ViewModel() {

    private val _optionState = MutableStateFlow(OptionsUiState())

    val uiState = combine(
        appDataRepository.tunnels.getTunnelConfigsFlow(),
        _optionState,
    ) { tunnels, optionState ->
        if (optionState.id != null) {
            val tunnelConfig = tunnels.fastFirstOrNull { it.id.toString() == optionState.id }
            val isPrimaryTunnel = tunnelConfig?.isPrimaryTunnel == true
            OptionsUiState(optionState.id, tunnelConfig, isPrimaryTunnel)
        } else OptionsUiState()
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(Constants.SUBSCRIPTION_TIMEOUT),
        OptionsUiState(),
    )

    fun init(tunnelId: String) {
        _optionState.value = _optionState.value.copy(
            id = tunnelId,
        )
    }

    fun onDeleteRunSSID(ssid: String) = viewModelScope.launch(Dispatchers.IO) {
        uiState.value.tunnel?.let {
            appDataRepository.tunnels.save(
                tunnelConfig = it.copy(
                    tunnelNetworks = (uiState.value.tunnel!!.tunnelNetworks - ssid).toMutableList(),
                ),
            )
        }
    }

    private fun saveTunnel(tunnelConfig: TunnelConfig?) = viewModelScope.launch(Dispatchers.IO) {
        tunnelConfig?.let {
            appDataRepository.tunnels.save(it)
        }
    }

    suspend fun onSaveRunSSID(ssid: String): Result<Unit> {
        val trimmed = ssid.trim()
        val tunnelsWithName = withContext(viewModelScope.coroutineContext) {
            appDataRepository.tunnels.findByTunnelNetworksName(trimmed)
        }
        return if (uiState.value.tunnel?.tunnelNetworks?.contains(trimmed) != true &&
            tunnelsWithName.isEmpty()) {
            uiState.value.tunnel?.tunnelNetworks?.add(trimmed)
            saveTunnel(uiState.value.tunnel)
            Result.Success(Unit)
        } else {
            Result.Error(Event.Error.SsidConflict)
        }
    }

    fun onToggleIsMobileDataTunnel() = viewModelScope.launch(Dispatchers.IO) {
        uiState.value.tunnel?.let {
            if (it.isMobileDataTunnel) {
                appDataRepository.tunnels.updateMobileDataTunnel(null)
            } else appDataRepository.tunnels.updateMobileDataTunnel(it)
        }
    }

    fun onTogglePrimaryTunnel() = viewModelScope.launch(Dispatchers.IO) {
        if (uiState.value.tunnel != null) {
            appDataRepository.tunnels.updatePrimaryTunnel(
                when (uiState.value.isDefaultTunnel) {
                    true -> null
                    false -> uiState.value.tunnel
                },
            )
            WireGuardAutoTunnel.requestTunnelTileServiceStateUpdate(WireGuardAutoTunnel.instance)
        }
    }
}
