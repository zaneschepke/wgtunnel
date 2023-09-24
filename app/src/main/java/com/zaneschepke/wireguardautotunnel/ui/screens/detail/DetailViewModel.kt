package com.zaneschepke.wireguardautotunnel.ui.screens.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wireguard.config.Config
import com.zaneschepke.wireguardautotunnel.repository.TunnelConfigDao
import com.zaneschepke.wireguardautotunnel.repository.model.TunnelConfig
import com.zaneschepke.wireguardautotunnel.service.tunnel.VpnService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class DetailViewModel @Inject constructor(private val tunnelRepo : TunnelConfigDao, private val vpnService : VpnService
) : ViewModel() {

    private val _tunnel = MutableStateFlow<Config?>(null)
    val tunnel get() = _tunnel.asStateFlow()

    private val _tunnelName = MutableStateFlow("")
    val tunnelName = _tunnelName.asStateFlow()
    val tunnelStats get() = vpnService.statistics
    val lastHandshake get() = vpnService.lastHandshake

    private suspend fun getTunnelConfigById(id: String): TunnelConfig? {
        return try {
            tunnelRepo.getById(id.toLong())
        } catch (e: Exception) {
            Timber.e(e.message)
            null
        }
    }
    fun emitConfig(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val tunnelConfig = getTunnelConfigById(id)
            if(tunnelConfig != null) {
                _tunnel.emit(TunnelConfig.configFromQuick(tunnelConfig.wgQuick))
            }
        }
    }
}