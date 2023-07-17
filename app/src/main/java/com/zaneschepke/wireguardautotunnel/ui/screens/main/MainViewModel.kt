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
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.repository.Repository
import com.zaneschepke.wireguardautotunnel.service.barcode.CodeScanner
import com.zaneschepke.wireguardautotunnel.service.foreground.Action
import com.zaneschepke.wireguardautotunnel.service.foreground.ServiceState
import com.zaneschepke.wireguardautotunnel.service.foreground.ServiceTracker
import com.zaneschepke.wireguardautotunnel.service.foreground.WireGuardConnectivityWatcherService
import com.zaneschepke.wireguardautotunnel.service.foreground.WireGuardTunnelService
import com.zaneschepke.wireguardautotunnel.service.tunnel.VpnService
import com.zaneschepke.wireguardautotunnel.service.tunnel.model.Settings
import com.zaneschepke.wireguardautotunnel.service.tunnel.model.TunnelConfig
import com.zaneschepke.wireguardautotunnel.ui.ViewState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject


@HiltViewModel
class MainViewModel @Inject constructor(private val application : Application,
                                        private val tunnelRepo : Repository<TunnelConfig>,
                                        private val settingsRepo : Repository<Settings>,
                                        private val vpnService: VpnService,
                                        private val codeScanner: CodeScanner
) : ViewModel() {

    private val _viewState = MutableStateFlow(ViewState())
    val viewState get() = _viewState.asStateFlow()
    val tunnels get() = tunnelRepo.itemFlow
    val state get() = vpnService.state

    val handshakeStatus get() = vpnService.handshakeStatus
    val tunnelName get() = vpnService.tunnelName
    private val _settings = MutableStateFlow(Settings())
    val settings get() = _settings.asStateFlow()

    private val defaultConfigName = {
        "tunnel${(Math.random() * 100000).toInt()}"
    }


    init {
        viewModelScope.launch {
            settingsRepo.itemFlow.collect {
                val settings = it.first()
                validateWatcherServiceState(settings)
                _settings.emit(settings)
            }
        }
    }

    private fun validateWatcherServiceState(settings: Settings) {
        val watcherState = ServiceTracker.getServiceState(application, WireGuardConnectivityWatcherService::class.java)
        if(settings.isAutoTunnelEnabled && watcherState == ServiceState.STOPPED && settings.defaultTunnel != null) {
            startWatcherService(settings.defaultTunnel!!)
        }
    }

    private fun startWatcherService(tunnel : String) {
        ServiceTracker.actionOnService(
            Action.START, application,
            WireGuardConnectivityWatcherService::class.java,
            mapOf(application.resources.getString(R.string.tunnel_extras_key) to tunnel))
    }

    fun onDelete(tunnel : TunnelConfig) {
        viewModelScope.launch {
            if(tunnelRepo.count() == 1L) {
                ServiceTracker.actionOnService( Action.STOP, application, WireGuardConnectivityWatcherService::class.java)
                val settings = settingsRepo.getAll()
                if(!settings.isNullOrEmpty()) {
                    val setting = settings[0]
                    setting.defaultTunnel = null
                    setting.isAutoTunnelEnabled = false
                    settingsRepo.save(setting)
                }
            }
            tunnelRepo.delete(tunnel)
        }
    }

    fun onTunnelStart(tunnelConfig : TunnelConfig) = viewModelScope.launch {
            ServiceTracker.actionOnService( Action.START, application, WireGuardTunnelService::class.java,
                mapOf(application.resources.getString(R.string.tunnel_extras_key) to tunnelConfig.toString()))
    }

    fun onTunnelStop() {
        ServiceTracker.actionOnService( Action.STOP, application, WireGuardTunnelService::class.java)
    }

    suspend fun onTunnelQRSelected() {
        codeScanner.scan().collect {
            if(!it.isNullOrEmpty() && it.contains(application.resources.getString(R.string.config_validation))) {
                tunnelRepo.save(TunnelConfig(name = defaultConfigName(), wgQuick = it))
            } else if(!it.isNullOrEmpty() && it.contains(application.resources.getString(R.string.barcode_downloading))) {
                showSnackBarMessage(application.resources.getString(R.string.barcode_downloading_message))
            } else {
                showSnackBarMessage(application.resources.getString(R.string.barcode_error))
            }
        }
    }

    fun onTunnelFileSelected(uri : Uri) {
        try {
            val fileName = getFileName(application.applicationContext, uri)
            val extension = getFileExtensionFromFileName(fileName)
            if(extension != ".conf") {
                viewModelScope.launch {
                    showSnackBarMessage(application.resources.getString(R.string.file_extension_message))
                }
                return
            }
            val stream = application.applicationContext.contentResolver.openInputStream(uri)
            stream ?: return
            val bufferReader = stream.bufferedReader(charset = Charsets.UTF_8)
                val config = Config.parse(bufferReader)
                val tunnelName = getNameFromFileName(fileName)
                viewModelScope.launch {
                    tunnelRepo.save(TunnelConfig(name = tunnelName, wgQuick = config.toWgQuickString()))
                }
            stream.close()
        } catch(_: BadConfigException) {
            viewModelScope.launch {
                showSnackBarMessage(application.applicationContext.getString(R.string.bad_config))
            }
        }
    }

    @SuppressLint("Range")
    private fun getFileName(context: Context, uri: Uri): String {
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor ?: return defaultConfigName()
            cursor.use {
                if(cursor.moveToFirst()) {
                    return cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                }
            }
        }
        return defaultConfigName()
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
        delay(3000)
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