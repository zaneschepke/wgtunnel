package com.zaneschepke.wireguardautotunnel.ui.screens.config

import android.Manifest
import android.app.Application
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wireguard.config.Config
import com.zaneschepke.wireguardautotunnel.repository.SettingsDoa
import com.zaneschepke.wireguardautotunnel.repository.TunnelConfigDao
import com.zaneschepke.wireguardautotunnel.service.shortcut.ShortcutsManager
import com.zaneschepke.wireguardautotunnel.repository.model.TunnelConfig
import com.zaneschepke.wireguardautotunnel.util.WgTunnelException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ConfigViewModel @Inject constructor(private val application : Application,
                                          private val tunnelRepo : TunnelConfigDao,
                                          private val settingsRepo : SettingsDoa) : ViewModel() {

    private val _tunnel = MutableStateFlow<TunnelConfig?>(null)
    private val _tunnelName = MutableStateFlow("")
    val tunnelName get() = _tunnelName.asStateFlow()
    val tunnel get() = _tunnel.asStateFlow()
    private val _packages = MutableStateFlow(emptyList<PackageInfo>())
    val packages get() = _packages.asStateFlow()
    private val packageManager = application.packageManager

    private val _checkedPackages = MutableStateFlow(mutableStateListOf<String>())
    val checkedPackages get() = _checkedPackages.asStateFlow()
    private val _include = MutableStateFlow(true)
    val include get() = _include.asStateFlow()

    private val _allApplications = MutableStateFlow(true)
    val allApplications get() = _allApplications.asStateFlow()

    fun emitScreenData(id : String) {
        viewModelScope.launch(Dispatchers.IO) {
            val tunnelConfig = getTunnelConfigById(id);
            emitTunnelConfig(tunnelConfig);
            emitTunnelConfigName(tunnelConfig?.name)
            emitQueriedPackages("")
            emitCurrentPackageConfigurations(id)
        }
    }

    private suspend fun getTunnelConfigById(id : String) : TunnelConfig? {
        return try {
            tunnelRepo.getById(id.toLong())
        } catch (e : Exception) {
            Timber.e(e.message)
            null
        }
    }

    private suspend fun emitTunnelConfig(tunnelConfig: TunnelConfig?) {
        if(tunnelConfig != null) {
            _tunnel.emit(tunnelConfig)
        }
    }

    private  suspend fun emitTunnelConfigName(name : String?) {
        if(name != null) {
            _tunnelName.emit(name)
        }
    }

    fun onTunnelNameChange(name : String) {
        _tunnelName.value = name
    }

    fun onIncludeChange(include : Boolean) {
        _include.value = include
    }
    fun onAddCheckedPackage(packageName : String) {
        _checkedPackages.value.add(packageName)
    }

    fun onAllApplicationsChange(allApplications : Boolean) {
        _allApplications.value = allApplications
    }

    fun onRemoveCheckedPackage(packageName : String) {
        _checkedPackages.value.remove(packageName)
    }

    private suspend fun emitSplitTunnelConfiguration(config : Config) {
        val excludedApps = config.`interface`.excludedApplications
        val includedApps = config.`interface`.includedApplications
        if (excludedApps.isNotEmpty() || includedApps.isNotEmpty()) {
            emitTunnelAllApplicationsDisabled()
            determineAppInclusionState(excludedApps, includedApps)
        } else {
            emitTunnelAllApplicationsEnabled()
        }
    }

    private suspend fun determineAppInclusionState(excludedApps : Set<String>, includedApps : Set<String>) {
        if (excludedApps.isEmpty()) {
            emitIncludedAppsExist()
            emitCheckedApps(includedApps)
        } else {
            emitExcludedAppsExist()
            emitCheckedApps(excludedApps)
        }
    }

    private suspend fun emitIncludedAppsExist() {
        _include.emit(true)
    }

    private suspend fun emitExcludedAppsExist() {
        _include.emit(false)
    }

    private suspend fun emitCheckedApps(apps : Set<String>) {
        _checkedPackages.emit(apps.toMutableStateList())
    }

    private suspend fun emitTunnelAllApplicationsEnabled() {
        _allApplications.emit(true)
    }

    private suspend fun emitTunnelAllApplicationsDisabled() {
        _allApplications.emit(false)
    }

    private fun emitCurrentPackageConfigurations(id : String) {
        viewModelScope.launch(Dispatchers.IO) {
            val tunnelConfig = getTunnelConfigById(id)
            if (tunnelConfig != null) {
                val config = TunnelConfig.configFromQuick(tunnelConfig.wgQuick)
                emitSplitTunnelConfiguration(config)
            }
        }
    }

    fun emitQueriedPackages(query : String) {
        viewModelScope.launch(Dispatchers.IO) {
            val packages = getAllInternetCapablePackages().filter {
                getPackageLabel(it).lowercase().contains(query.lowercase())
            }
            _packages.emit(packages)
        }
    }

    fun getPackageLabel(packageInfo : PackageInfo) : String {
        return packageInfo.applicationInfo.loadLabel(application.packageManager).toString()
    }


    private fun getAllInternetCapablePackages() : List<PackageInfo> {
        return getPackagesHoldingPermissions(arrayOf(Manifest.permission.INTERNET))
    }

    private fun getPackagesHoldingPermissions(permissions: Array<String>): List<PackageInfo> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackagesHoldingPermissions(permissions, PackageManager.PackageInfoFlags.of(0L))
        } else {
            packageManager.getPackagesHoldingPermissions(permissions, 0)
        }
    }

    private fun removeTunnelShortcuts(tunnelConfig: TunnelConfig?) {
        if(tunnelConfig != null) {
            ShortcutsManager.removeTunnelShortcuts(application, tunnelConfig)
        }

    }

    private fun isAllApplicationsEnabled() : Boolean {
        return _allApplications.value
    }

    private fun isIncludeApplicationsEnabled() : Boolean {
        return _include.value
    }

    private fun updateQuickStringWithSelectedPackages() : String {
        var wgQuick = _tunnel.value?.wgQuick
        if(wgQuick != null) {
            wgQuick = if(isAllApplicationsEnabled()) {
                TunnelConfig.clearAllApplicationsFromConfig(wgQuick)
            } else if(isIncludeApplicationsEnabled()) {
                TunnelConfig.setIncludedApplicationsOnQuick(_checkedPackages.value, wgQuick)
            } else {
                TunnelConfig.setExcludedApplicationsOnQuick(_checkedPackages.value, wgQuick)
            }
        } else {
            throw WgTunnelException("Wg quick string is null")
        }
        return wgQuick;
    }

    private suspend fun saveConfig(tunnelConfig: TunnelConfig) {
        tunnelRepo.save(tunnelConfig)
    }
    private suspend fun updateTunnelConfig(tunnelConfig: TunnelConfig?) {
        if(tunnelConfig != null) {
            saveConfig(tunnelConfig)
            addTunnelShortcuts(tunnelConfig)
            updateSettingsDefaultTunnel(tunnelConfig)
        }
    }

    private suspend fun updateSettingsDefaultTunnel(tunnelConfig: TunnelConfig) {
        val settings = settingsRepo.getAll()
        if(settings.isNotEmpty()) {
            val setting = settings[0]
            if(setting.defaultTunnel != null) {
                if(tunnelConfig.id == TunnelConfig.from(setting.defaultTunnel!!).id) {
                    settingsRepo.save(setting.copy(
                        defaultTunnel = tunnelConfig.toString()
                    ))
                }
            }
        }
    }

    private fun addTunnelShortcuts(tunnelConfig: TunnelConfig) {
        ShortcutsManager.createTunnelShortcuts(application, tunnelConfig)
    }

    suspend fun onSaveAllChanges() {
        try {
            removeTunnelShortcuts(_tunnel.value)
            val wgQuick = updateQuickStringWithSelectedPackages()
            val tunnelConfig = _tunnel.value?.copy(
                name = _tunnelName.value,
                wgQuick = wgQuick
            )
            updateTunnelConfig(tunnelConfig)
        } catch (e : Exception) {
            Timber.e(e.message)
        }
    }
}