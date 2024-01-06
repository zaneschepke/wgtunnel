package com.zaneschepke.wireguardautotunnel.ui.screens.main

import android.app.Application
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wireguard.config.Config
import com.zaneschepke.wireguardautotunnel.WireGuardAutoTunnel
import com.zaneschepke.wireguardautotunnel.data.model.Settings
import com.zaneschepke.wireguardautotunnel.data.model.TunnelConfig
import com.zaneschepke.wireguardautotunnel.data.repository.SettingsRepository
import com.zaneschepke.wireguardautotunnel.data.repository.TunnelConfigRepository
import com.zaneschepke.wireguardautotunnel.service.foreground.ServiceManager
import com.zaneschepke.wireguardautotunnel.service.tunnel.VpnService
import com.zaneschepke.wireguardautotunnel.util.Constants
import com.zaneschepke.wireguardautotunnel.util.Event
import com.zaneschepke.wireguardautotunnel.util.NumberUtils
import com.zaneschepke.wireguardautotunnel.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.InputStream
import java.util.zip.ZipInputStream
import javax.inject.Inject

@HiltViewModel
class MainViewModel
@Inject
constructor(
    private val application: Application,
    private val tunnelConfigRepository: TunnelConfigRepository,
    private val settingsRepository: SettingsRepository,
    private val vpnService: VpnService
) : ViewModel() {

    val uiState =
        combine(
                settingsRepository.getSettingsFlow(),
                tunnelConfigRepository.getTunnelConfigsFlow(),
                vpnService.vpnState,
            ) { settings, tunnels, vpnState ->
                validateWatcherServiceState(settings)
                MainUiState(settings, tunnels, vpnState, false)
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(Constants.SUBSCRIPTION_TIMEOUT),
                MainUiState(),
            )

    private fun validateWatcherServiceState(settings: Settings) =
        viewModelScope.launch(Dispatchers.IO) {
            if (settings.isAutoTunnelEnabled) {
                ServiceManager.startWatcherService(application.applicationContext)
            }
        }

    private fun stopWatcherService() =
        viewModelScope.launch(Dispatchers.IO) {
            ServiceManager.stopWatcherService(application.applicationContext)
        }

    fun onDelete(tunnel: TunnelConfig) {
        viewModelScope.launch(Dispatchers.IO) {
            if (tunnelConfigRepository.count() == 1) {
                stopWatcherService()
                val settings = settingsRepository.getSettings()
                settings.defaultTunnel = null
                settings.isAutoTunnelEnabled = false
                settings.isAlwaysOnVpnEnabled = false
                saveSettings(settings)
            }
            tunnelConfigRepository.delete(tunnel)
            WireGuardAutoTunnel.requestTileServiceStateUpdate()
        }
    }

    fun onTunnelStart(tunnelConfig: TunnelConfig) =
        viewModelScope.launch(Dispatchers.IO) {
            Timber.d("On start called!")
            stopActiveTunnel().await()
            startTunnel(tunnelConfig)
        }

    private fun startTunnel(tunnelConfig: TunnelConfig) =
        viewModelScope.launch(Dispatchers.IO) {
            Timber.d("Start tunnel via manager")
            ServiceManager.startVpnService(application.applicationContext, tunnelConfig.toString())
        }

    private fun stopActiveTunnel() =
        viewModelScope.async(Dispatchers.IO) {
            onTunnelStop()
            delay(Constants.TOGGLE_TUNNEL_DELAY)
        }

    fun onTunnelStop() =
        viewModelScope.launch(Dispatchers.IO) {
            Timber.d("Stopping active tunnel")
            ServiceManager.stopVpnService(application.applicationContext)
        }

    private fun validateConfigString(config: String) {
        TunnelConfig.configFromQuick(config)
    }

    suspend fun onTunnelQrResult(result: String): Result<Unit> {
        return try {
            validateConfigString(result)
            val tunnelConfig =
                TunnelConfig(name = NumberUtils.generateRandomTunnelName(), wgQuick = result)
            addTunnel(tunnelConfig)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(Event.Error.InvalidQrCode)
        }
    }

    private suspend fun saveTunnelConfigFromStream(stream: InputStream, fileName: String) {
        val bufferReader = stream.bufferedReader(charset = Charsets.UTF_8)
        val config = Config.parse(bufferReader)
        val tunnelName = getNameFromFileName(fileName)
        addTunnel(TunnelConfig(name = tunnelName, wgQuick = config.toWgQuickString()))
        withContext(Dispatchers.IO) { stream.close() }
    }

    private fun getInputStreamFromUri(uri: Uri): InputStream? {
        return application.applicationContext.contentResolver.openInputStream(uri)
    }

    suspend fun onTunnelFileSelected(uri: Uri): Result<Unit> {
        try {
            if (isValidUriContentScheme(uri)) {
                val fileName = getFileName(application.applicationContext, uri)
                when (getFileExtensionFromFileName(fileName)) {
                    Constants.CONF_FILE_EXTENSION ->
                        saveTunnelFromConfUri(fileName, uri).let {
                            when (it) {
                                is Result.Error -> return Result.Error(Event.Error.FileReadFailed)
                                is Result.Success -> return it
                            }
                        }
                    Constants.ZIP_FILE_EXTENSION -> saveTunnelsFromZipUri(uri)
                    else -> return Result.Error(Event.Error.InvalidFileExtension)
                }
                return Result.Success(Unit)
            } else {
                return Result.Error(Event.Error.InvalidFileExtension)
            }
        } catch (e: Exception) {
            return Result.Error(Event.Error.FileReadFailed)
        }
    }

    private suspend fun saveTunnelsFromZipUri(uri: Uri) {
        ZipInputStream(getInputStreamFromUri(uri)).use { zip ->
            generateSequence { zip.nextEntry }
                .filterNot {
                    it.isDirectory ||
                        getFileExtensionFromFileName(it.name) != Constants.CONF_FILE_EXTENSION
                }
                .forEach {
                    val name = getNameFromFileName(it.name)
                    val config = Config.parse(zip)
                    viewModelScope.launch(Dispatchers.IO) {
                        addTunnel(TunnelConfig(name = name, wgQuick = config.toWgQuickString()))
                    }
                }
        }
    }

    private suspend fun saveTunnelFromConfUri(name: String, uri: Uri): Result<Unit> {
        val stream = getInputStreamFromUri(uri)
        return if (stream != null) {
            saveTunnelConfigFromStream(stream, name)
            Result.Success(Unit)
        } else {
            Result.Error(Event.Error.FileReadFailed)
        }
    }

    private suspend fun addTunnel(tunnelConfig: TunnelConfig) {
        saveTunnel(tunnelConfig)
        WireGuardAutoTunnel.requestTileServiceStateUpdate()
    }

    fun pauseAutoTunneling() =
        viewModelScope.launch {
            settingsRepository.save(uiState.value.settings.copy(isAutoTunnelPaused = true))
        }

    fun resumeAutoTunneling() =
        viewModelScope.launch {
            settingsRepository.save(uiState.value.settings.copy(isAutoTunnelPaused = false))
        }

    private suspend fun saveTunnel(tunnelConfig: TunnelConfig) {
        tunnelConfigRepository.save(tunnelConfig)
    }

    private fun getFileNameByCursor(context: Context, uri: Uri): String? {
        context.contentResolver.query(uri, null, null, null, null)?.use {
            return getDisplayNameByCursor(it)
        }
        return null
    }

    private fun getDisplayNameColumnIndex(cursor: Cursor): Int? {
        val columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        return if (columnIndex != -1) {
            return columnIndex
        } else {
            null
        }
    }

    private fun getDisplayNameByCursor(cursor: Cursor): String? {
        return if (cursor.moveToFirst()) {
            val index = getDisplayNameColumnIndex(cursor)
            if (index != null) {
                cursor.getString(index)
            } else null
        } else null
    }

    private fun isValidUriContentScheme(uri: Uri): Boolean {
        return uri.scheme == Constants.URI_CONTENT_SCHEME
    }

    private fun getFileName(context: Context, uri: Uri): String {
        return getFileNameByCursor(context, uri) ?: NumberUtils.generateRandomTunnelName()
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

    private fun saveSettings(settings: Settings) =
        viewModelScope.launch(Dispatchers.IO) { settingsRepository.save(settings) }

    fun onDefaultTunnelChange(selectedTunnel: TunnelConfig?) =
        viewModelScope.launch {
            if (selectedTunnel != null) {
                saveSettings(uiState.value.settings.copy(defaultTunnel = selectedTunnel.toString()))
                    .join()
                WireGuardAutoTunnel.requestTileServiceStateUpdate()
            }
        }
}
