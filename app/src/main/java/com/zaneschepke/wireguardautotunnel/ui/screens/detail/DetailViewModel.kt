package com.zaneschepke.wireguardautotunnel.ui.screens.detail

import androidx.lifecycle.ViewModel
import com.wireguard.config.Config
import com.zaneschepke.wireguardautotunnel.repository.TunnelConfigDao
import com.zaneschepke.wireguardautotunnel.repository.model.TunnelConfig
import com.zaneschepke.wireguardautotunnel.service.tunnel.VpnService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class DetailViewModel @Inject constructor(private val tunnelRepo : TunnelConfigDao, private val vpnService : VpnService

) : ViewModel() {

    private val _tunnel = MutableStateFlow<Config?>(null)
    val tunnel get() = _tunnel.asStateFlow()

    private val _tunnelName = MutableStateFlow<String>("")
    val tunnelName = _tunnelName.asStateFlow()
    val tunnelStats get() = vpnService.statistics
    val lastHandshake get() = vpnService.lastHandshake

    private var config : TunnelConfig? = null

    suspend fun getTunnelById(id : String?) : TunnelConfig? {
        return try {
            if(id != null) {
               config = tunnelRepo.getById(id.toLong())
                if (config != null) {
                    _tunnel.emit(TunnelConfig.configFromQuick(config!!.wgQuick))
                    _tunnelName.emit(config!!.name)
                }
                return config
            }
            return null
        } catch (e : Exception) {
            Timber.e(e.message)
            null
        }
    }
}