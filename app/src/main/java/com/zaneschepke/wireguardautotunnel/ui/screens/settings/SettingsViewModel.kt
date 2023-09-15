package com.zaneschepke.wireguardautotunnel.ui.screens.settings

import android.app.Application
import android.content.Context
import android.location.LocationManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.repository.SettingsDoa
import com.zaneschepke.wireguardautotunnel.repository.TunnelConfigDao
import com.zaneschepke.wireguardautotunnel.repository.model.Settings
import com.zaneschepke.wireguardautotunnel.repository.model.TunnelConfig
import com.zaneschepke.wireguardautotunnel.service.foreground.ServiceManager
import com.zaneschepke.wireguardautotunnel.ui.ViewState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
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
    private val _viewState = MutableStateFlow(ViewState())
    val viewState get() = _viewState.asStateFlow()

    init {
        checkLocationServicesEnabled()
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
            showSnackBarMessage("SSID already exists.")
        }
    }

    suspend fun onDefaultTunnelSelected(tunnelConfig: TunnelConfig) {
        settingsRepo.save(_settings.value.copy(
            defaultTunnel = tunnelConfig.toString()
        ))
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

    suspend fun toggleAutoTunnel() {
        if(_settings.value.defaultTunnel.isNullOrEmpty() && !_settings.value.isAutoTunnelEnabled) {
            showSnackBarMessage(application.getString(R.string.select_tunnel_message))
            return
        }
        if(_settings.value.isAutoTunnelEnabled) {
            ServiceManager.stopWatcherService(application)
        } else {
            if(_settings.value.defaultTunnel != null) {
                val defaultTunnel = _settings.value.defaultTunnel
                ServiceManager.startWatcherService(application, defaultTunnel!!)
            }
        }
        settingsRepo.save(_settings.value.copy(
            isAutoTunnelEnabled = !_settings.value.isAutoTunnelEnabled
        ))
    }

    suspend fun showSnackBarMessage(message : String) {
        _viewState.emit(_viewState.value.copy(
            showSnackbarMessage = true,
            snackbarMessage = message,
            snackbarActionText = "Okay",
            onSnackbarActionClick = {
                viewModelScope.launch {
                    dismissSnackBar()
                }
            }
        ))
    }

    private suspend fun dismissSnackBar() {
        _viewState.emit(_viewState.value.copy(
            showSnackbarMessage = false
        ))
    }

    suspend fun onToggleAlwaysOnVPN() {
        if(_settings.value.defaultTunnel != null) {
            _settings.emit(
                _settings.value.copy(isAlwaysOnVpnEnabled = !_settings.value.isAlwaysOnVpnEnabled)
            )
            settingsRepo.save(_settings.value)
        } else {
            showSnackBarMessage(application.getString(R.string.select_tunnel_message))
        }
    }
    fun checkLocationServicesEnabled() : Boolean {
        val locationManager =
            application.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }
}