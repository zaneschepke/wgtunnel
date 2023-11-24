package com.zaneschepke.wireguardautotunnel.ui.screens.main

import android.app.Application
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import com.zaneschepke.wireguardautotunnel.service.foreground.WireGuardTunnelService
import com.zaneschepke.wireguardautotunnel.service.tunnel.VpnService
import com.zaneschepke.wireguardautotunnel.util.NumberUtils
import com.zaneschepke.wireguardautotunnel.util.WgTunnelException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.util.zip.ZipInputStream
import javax.inject.Inject


@HiltViewModel
class MainViewModel @Inject constructor(
    private val application: Application,
    private val tunnelRepo: TunnelConfigDao,
    private val settingsRepo: SettingsDoa,
    private val vpnService: VpnService
) : ViewModel() {

    val tunnels get() = tunnelRepo.getAllFlow()
    val state get() = vpnService.state

    val handshakeStatus get() = vpnService.handshakeStatus
    val tunnelName get() = vpnService.tunnelName
    private val _settings = MutableStateFlow(Settings())
    val settings get() = _settings.asStateFlow()
    val statistics get() = vpnService.statistics

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
        val watcherState = ServiceManager.getServiceState(
            application.applicationContext,
            WireGuardConnectivityWatcherService::class.java
        )
        if (settings.isAutoTunnelEnabled && watcherState == ServiceState.STOPPED && settings.defaultTunnel != null) {
            ServiceManager.startWatcherService(
                application.applicationContext,
                settings.defaultTunnel!!
            )
        }
    }


    fun onDelete(tunnel: TunnelConfig) {
        viewModelScope.launch {
            if (tunnelRepo.count() == 1L) {
                ServiceManager.stopWatcherService(application.applicationContext)
                val settings = settingsRepo.getAll()
                if (settings.isNotEmpty()) {
                    val setting = settings[0]
                    setting.defaultTunnel = null
                    setting.isAutoTunnelEnabled = false
                    setting.isAlwaysOnVpnEnabled = false
                    settingsRepo.save(setting)
                }
            }
            tunnelRepo.delete(tunnel)
        }
    }

    fun onTunnelStart(tunnelConfig: TunnelConfig) {
        viewModelScope.launch {
            stopActiveTunnel()
            startTunnel(tunnelConfig)
        }
    }

    private fun startTunnel(tunnelConfig: TunnelConfig) {
        ServiceManager.startVpnService(application.applicationContext, tunnelConfig.toString())
    }

    private suspend fun stopActiveTunnel() {
        if (ServiceManager.getServiceState(
                application.applicationContext,
                WireGuardTunnelService::class.java,
            ) == ServiceState.STARTED
        ) {
            onTunnelStop()
            delay(Constants.TOGGLE_TUNNEL_DELAY)
        }
    }

    fun onTunnelStop() {
        ServiceManager.stopVpnService(application.applicationContext)
    }

    private fun validateConfigString(config: String) {
        TunnelConfig.configFromQuick(config)
    }

    suspend fun onTunnelQrResult(result: String) {
        try {
            validateConfigString(result)
            val tunnelConfig =
                TunnelConfig(name = NumberUtils.generateRandomTunnelName(), wgQuick = result)
            addTunnel(tunnelConfig)
        } catch (e : Exception) {
            throw WgTunnelException(e)
        }
    }

    private suspend fun saveTunnelConfigFromStream(stream: InputStream, fileName: String) {
        val bufferReader = stream.bufferedReader(charset = Charsets.UTF_8)
        val config = Config.parse(bufferReader)
        val tunnelName = getNameFromFileName(fileName)
        addTunnel(TunnelConfig(name = tunnelName, wgQuick = config.toWgQuickString()))
        withContext(Dispatchers.IO) {
            stream.close()
        }
    }

    private fun getInputStreamFromUri(uri: Uri): InputStream {
        return application.applicationContext.contentResolver.openInputStream(uri)
            ?: throw WgTunnelException(application.getString(R.string.stream_failed))
    }

    suspend fun onTunnelFileSelected(uri: Uri) {
        try {
            val fileName = getFileName(application.applicationContext, uri)
            val fileExtension = getFileExtensionFromFileName(fileName)
            when(fileExtension){
                Constants.CONF_FILE_EXTENSION -> saveTunnelFromConfUri(fileName, uri)
                Constants.ZIP_FILE_EXTENSION -> saveTunnelsFromZipUri(uri)
                else -> throw WgTunnelException(application.getString(R.string.file_extension_message))
            }
        } catch (e: Exception) {
            throw WgTunnelException(e)
        }
    }

    private suspend fun saveTunnelsFromZipUri(uri: Uri) {
        ZipInputStream(getInputStreamFromUri(uri)).use { zip ->
            generateSequence { zip.nextEntry }
                .filterNot { it.isDirectory ||
                        getFileExtensionFromFileName(it.name) != Constants.CONF_FILE_EXTENSION }
                .forEach {
                    val name = getNameFromFileName(it.name)
                    val config = Config.parse(zip)
                    viewModelScope.launch(Dispatchers.IO) {
                        addTunnel(TunnelConfig(name = name, wgQuick = config.toWgQuickString()))
                }
            }
        }
    }

    private suspend fun saveTunnelFromConfUri(name : String, uri: Uri) {
        val stream = getInputStreamFromUri(uri)
        saveTunnelConfigFromStream(stream, name)
    }

    private suspend fun addTunnel(tunnelConfig: TunnelConfig) {
        saveTunnel(tunnelConfig)
    }

    private suspend fun saveTunnel(tunnelConfig: TunnelConfig) {
        tunnelRepo.save(tunnelConfig)
    }

    private fun getFileNameByCursor(context: Context, uri: Uri): String {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        if (cursor != null) {
            cursor.use {
                return getDisplayNameByCursor(it)
            }
        } else {
            throw WgTunnelException("Failed to initialize cursor")
        }
    }

    private fun getDisplayNameColumnIndex(cursor: Cursor): Int {
        val columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (columnIndex == -1) {
            throw WgTunnelException("Cursor out of bounds")
        }
        return columnIndex
    }

    private fun getDisplayNameByCursor(cursor: Cursor): String {
        if (cursor.moveToFirst()) {
            val index = getDisplayNameColumnIndex(cursor)
            return cursor.getString(index)
        } else {
            throw WgTunnelException("Cursor failed to move to first")
        }
    }

    private fun validateUriContentScheme(uri: Uri) {
        if (uri.scheme != Constants.URI_CONTENT_SCHEME) {
            throw WgTunnelException(application.getString(R.string.file_extension_message))
        }
    }


    private fun getFileName(context: Context, uri: Uri): String {
        validateUriContentScheme(uri)
        return try {
            getFileNameByCursor(context, uri)
        } catch (_: Exception) {
            NumberUtils.generateRandomTunnelName()
        }
    }

    private fun getNameFromFileName(fileName: String): String {
        return fileName.substring(0, fileName.lastIndexOf('.'))
    }

    private fun getFileExtensionFromFileName(fileName: String): String {
        return try {
            fileName.substring(fileName.lastIndexOf('.'))
        } catch (e: Exception) {
            ""
        }
    }

    suspend fun onDefaultTunnelChange(selectedTunnel: TunnelConfig?) {
        if (selectedTunnel != null) {
            _settings.emit(
                _settings.value.copy(
                    defaultTunnel = selectedTunnel.toString()
                )
            )
            settingsRepo.save(_settings.value)
        }
    }
}