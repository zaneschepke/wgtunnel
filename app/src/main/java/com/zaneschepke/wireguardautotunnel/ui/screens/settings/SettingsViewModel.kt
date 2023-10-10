package com.zaneschepke.wireguardautotunnel.ui.screens.settings

import android.app.Application
import android.content.Context
import android.location.LocationManager
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zaneschepke.wireguardautotunnel.repository.SettingsDoa
import com.zaneschepke.wireguardautotunnel.repository.TunnelConfigDao
import com.zaneschepke.wireguardautotunnel.repository.model.Settings
import com.zaneschepke.wireguardautotunnel.repository.model.TunnelConfig
import com.zaneschepke.wireguardautotunnel.service.foreground.ServiceManager
import com.zaneschepke.wireguardautotunnel.util.WgTunnelException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class SettingsViewModel @Inject constructor(private val application : Application,
                                            private val tunnelRepo : TunnelConfigDao, private val settingsRepo : SettingsDoa
) : ViewModel() {

    private val _trustedSSIDs = MutableStateFlow(emptyList<String>())
    val trustedSSIDs = _trustedSSIDs.asStateFlow()
    private val _settings = MutableStateFlow(Settings())
    val settings get() = _settings.asStateFlow()
    val tunnels get() = tunnelRepo.getAllFlow()
    init {
        isLocationServicesEnabled()
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepo.getAllFlow().filter { it.isNotEmpty() }.collect {
                val settings = it.first()
                _settings.emit(settings)
                _trustedSSIDs.emit(settings.trustedNetworkSSIDs.toList())
            }
        }
    }

    suspend fun onSaveTrustedSSID(ssid: String) {
        val trimmed = ssid.trim()
        if (!_settings.value.trustedNetworkSSIDs.contains(trimmed)) {
            _settings.value.trustedNetworkSSIDs.add(trimmed)
            settingsRepo.save(_settings.value)
        } else {
            throw WgTunnelException("SSID already exists.")
        }
    }

    suspend fun onToggleTunnelOnMobileData() {
        settingsRepo.save(_settings.value.copy(
            isTunnelOnMobileDataEnabled = !_settings.value.isTunnelOnMobileDataEnabled
        ))
    }

    suspend fun onDeleteTrustedSSID(ssid: String) {
        _settings.value.trustedNetworkSSIDs.remove(ssid)
        settingsRepo.save(_settings.value)
    }

    private fun emitFirstTunnelAsDefault() = viewModelScope.async {
        _settings.emit(_settings.value.copy(defaultTunnel = getFirstTunnelConfig().toString()))
    }

    suspend fun toggleAutoTunnel() {
        if(_settings.value.isAutoTunnelEnabled) {
            ServiceManager.stopWatcherService(application)
        } else {
            if(_settings.value.defaultTunnel == null) {
                emitFirstTunnelAsDefault().await()
            }
            val defaultTunnel = _settings.value.defaultTunnel
            ServiceManager.startWatcherService(application, defaultTunnel!!)
        }
        settingsRepo.save(_settings.value.copy(
            isAutoTunnelEnabled = !_settings.value.isAutoTunnelEnabled
        ))
    }

    private suspend fun getFirstTunnelConfig() : TunnelConfig {
        return tunnelRepo.getAll().first();
    }

    suspend fun onToggleAlwaysOnVPN() {
        if(_settings.value.defaultTunnel == null) {
            emitFirstTunnelAsDefault().await()
        }
        val updatedSettings = _settings.value.copy(isAlwaysOnVpnEnabled = !_settings.value.isAlwaysOnVpnEnabled)
        emitSettings(updatedSettings)
        saveSettings(updatedSettings)
    }

    private suspend fun emitSettings(settings: Settings) {
        _settings.emit(
            settings
        )
    }

    private suspend fun saveSettings(settings: Settings) {
        settingsRepo.save(settings)
    }

    suspend fun onToggleTunnelOnEthernet() {
        if(_settings.value.defaultTunnel == null) {
            emitFirstTunnelAsDefault().await()
        }
        _settings.emit(
            _settings.value.copy(isTunnelOnEthernetEnabled = !_settings.value.isTunnelOnEthernetEnabled)
        )
        settingsRepo.save(_settings.value)
    }

    private fun isLocationServicesEnabled() : Boolean {
        val locationManager =
            application.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    fun isLocationServicesNeeded() : Boolean {
        return(!isLocationServicesEnabled() && Build.VERSION.SDK_INT > Build.VERSION_CODES.P)
    }
}