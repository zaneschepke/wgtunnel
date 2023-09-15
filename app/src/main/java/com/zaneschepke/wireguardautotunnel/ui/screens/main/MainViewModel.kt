package com.zaneschepke.wireguardautotunnel.ui.screens.main

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wireguard.config.BadConfigException
import com.wireguard.config.Config
import com.zaneschepke.wireguardautotunnel.Constants
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.repository.SettingsDoa
import com.zaneschepke.wireguardautotunnel.repository.TunnelConfigDao
import com.zaneschepke.wireguardautotunnel.repository.model.Settings
import com.zaneschepke.wireguardautotunnel.repository.model.TunnelConfig
import com.zaneschepke.wireguardautotunnel.service.foreground.ServiceManager
import com.zaneschepke.wireguardautotunnel.service.foreground.ServiceState
import com.zaneschepke.wireguardautotunnel.service.foreground.WireGuardConnectivityWatcherService
import com.zaneschepke.wireguardautotunnel.service.shortcut.ShortcutsManager
import com.zaneschepke.wireguardautotunnel.service.tunnel.VpnService
import com.zaneschepke.wireguardautotunnel.ui.ViewState
import com.zaneschepke.wireguardautotunnel.util.NumberUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject


@HiltViewModel
class MainViewModel @Inject constructor(private val application : Application,
                                        private val tunnelRepo : TunnelConfigDao,
                                        private val settingsRepo : SettingsDoa,
                                        private val vpnService: VpnService
) : ViewModel() {

    private val _viewState = MutableStateFlow(ViewState())
    val viewState get() = _viewState.asStateFlow()
    val tunnels get() = tunnelRepo.getAllFlow()
    val state get() = vpnService.state

    val handshakeStatus get() = vpnService.handshakeStatus
    val tunnelName get() = vpnService.tunnelName
    private val _settings = MutableStateFlow(Settings())
    val settings get() = _settings.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepo.getAllFlow().filter { it.isNotEmpty() }.collect {
                val settings = it.first()
                validateWatcherServiceState(settings)
                _settings.emit(settings)
            }
        }
    }

    private fun validateWatcherServiceState(settings: Settings) {
        val watcherState = ServiceManager.getServiceState(application.applicationContext, WireGuardConnectivityWatcherService::class.java)
        if(settings.isAutoTunnelEnabled && watcherState == ServiceState.STOPPED && settings.defaultTunnel != null) {
            ServiceManager.startWatcherService(application.applicationContext, settings.defaultTunnel!!)
        }
    }


    fun onDelete(tunnel : TunnelConfig) {
        viewModelScope.launch {
            if(tunnelRepo.count() == 1L) {
                ServiceManager.stopWatcherService(application.applicationContext)
                val settings = settingsRepo.getAll()
                if(settings.isNotEmpty()) {
                    val setting = settings[0]
                    setting.defaultTunnel = null
                    setting.isAutoTunnelEnabled = false
                    setting.isAlwaysOnVpnEnabled = false
                    settingsRepo.save(setting)
                }
            }
            tunnelRepo.delete(tunnel)
            ShortcutsManager.removeTunnelShortcuts(application.applicationContext, tunnel)
        }
    }

    fun onTunnelStart(tunnelConfig : TunnelConfig) = viewModelScope.launch {
        ServiceManager.startVpnService(application.applicationContext, tunnelConfig.toString())
    }

    fun onTunnelStop() {
        ServiceManager.stopVpnService(application.applicationContext)
    }

    fun onTunnelQrResult(result : String) {
        viewModelScope.launch(Dispatchers.IO) {
        if(result.contains(application.resources.getString(R.string.config_validation))) {
            val tunnelConfig =
                TunnelConfig(name = NumberUtils.generateRandomTunnelName(), wgQuick = result)
                saveTunnel(tunnelConfig)
            } else {
                showSnackBarMessage(application.resources.getString(R.string.barcode_error))
            }
        }
    }

    fun onTunnelFileSelected(uri : Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val fileName = getFileName(application.applicationContext, uri)
                val extension = getFileExtensionFromFileName(fileName)
                if (extension != ".conf") {
                    launch {
                        showSnackBarMessage(application.resources.getString(R.string.file_extension_message))
                    }
                    return@launch
                }
                val stream = application.applicationContext.contentResolver.openInputStream(uri)
                stream ?: return@launch
                val bufferReader = stream.bufferedReader(charset = Charsets.UTF_8)
                val config = Config.parse(bufferReader)
                val tunnelName = getNameFromFileName(fileName)
                saveTunnel(TunnelConfig(name = tunnelName, wgQuick = config.toWgQuickString()))
                stream.close()
            } catch (_: BadConfigException) {
                launch {
                    showSnackBarMessage(application.applicationContext.getString(R.string.bad_config))
                }
            }
        }
    }

    private suspend fun saveTunnel(tunnelConfig : TunnelConfig) {
        tunnelRepo.save(tunnelConfig)
        ShortcutsManager.createTunnelShortcuts(application.applicationContext, tunnelConfig)
    }

    @SuppressLint("Range")
    private fun getFileName(context: Context, uri: Uri): String {
        if (uri.scheme == "content") {
            val cursor = try {
                context.contentResolver.query(uri, null, null, null, null)
            } catch (e : Exception) {
                Timber.d("Exception getting config name")
                null
            }
            cursor ?: return NumberUtils.generateRandomTunnelName()
            cursor.use {
                if(cursor.moveToFirst()) {
                    return cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                }
            }
        }
        return NumberUtils.generateRandomTunnelName()
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
        delay(Constants.SNACKBAR_DELAY)
        dismissSnackBar()
    }

    private suspend fun dismissSnackBar() {
        _viewState.emit(_viewState.value.copy(
            showSnackbarMessage = false
        ))
    }

    private fun getNameFromFileName(fileName : String) : String {
        return fileName.substring(0 , fileName.lastIndexOf('.') )
    }

    private fun getFileExtensionFromFileName(fileName : String) : String {
        return try {
            fileName.substring(fileName.lastIndexOf('.'))
        } catch (e : Exception) {
            ""
        }
    }
}