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
import com.zaneschepke.wireguardautotunnel.repository.Repository
import com.zaneschepke.wireguardautotunnel.service.tunnel.model.Settings
import com.zaneschepke.wireguardautotunnel.service.tunnel.model.TunnelConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ConfigViewModel @Inject constructor(private val application : Application,
                                          private val tunnelRepo : Repository<TunnelConfig>,
                                          private val settingsRepo : Repository<Settings>) : ViewModel() {

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

    suspend fun getTunnelById(id : String?) : TunnelConfig? {
        return try {
            if(id != null) {
                val config = tunnelRepo.getById(id.toLong())
                if (config != null) {
                    _tunnel.emit(config)
                    _tunnelName.emit(config.name)

                }
                return config
            }
            return null
        } catch (e : Exception) {
            Timber.e(e.message)
            null
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

    suspend fun emitCurrentPackageConfigurations(id : String?) {
        val tunnelConfig = getTunnelById(id)
        if(tunnelConfig != null) {
            val config = TunnelConfig.configFromQuick(tunnelConfig.wgQuick)
            val excludedApps = config.`interface`.excludedApplications
            val includedApps = config.`interface`.includedApplications
            if(excludedApps.isNullOrEmpty() && includedApps.isNullOrEmpty()) {
                _allApplications.emit(true)
                return
            }
            if(excludedApps.isEmpty()) {
                _include.emit(true)
                _checkedPackages.emit(includedApps.toMutableStateList())
            } else {
                _include.emit(false)
                _checkedPackages.emit(excludedApps.toMutableStateList())
            }
            _allApplications.emit(false)
        }
    }

    fun emitQueriedPackages(query : String) {
        viewModelScope.launch {
            _packages.emit(getAllInternetCapablePackages().filter {
                it.packageName.contains(query)
            })
        }
    }

    private fun getAllInternetCapablePackages() : List<PackageInfo> {
        return getPackagesHoldingPermissions(arrayOf(Manifest.permission.INTERNET))
    }

    private fun getPackagesHoldingPermissions(permissions: Array<String>): List<PackageInfo> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackagesHoldingPermissions(permissions, PackageManager.PackageInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackagesHoldingPermissions(permissions, 0)
        }
    }

    suspend fun onSaveAllChanges() {
        var wgQuick = _tunnel.value?.wgQuick
        if(wgQuick != null) {
            wgQuick = if(_include.value) {
                TunnelConfig.setIncludedApplicationsOnQuick(_checkedPackages.value, wgQuick)
            } else {
                TunnelConfig.setExcludedApplicationsOnQuick(_checkedPackages.value, wgQuick)
            }
            if(_allApplications.value) {
                wgQuick = TunnelConfig.clearAllApplicationsFromConfig(wgQuick)
            }
            _tunnel.value?.copy(
                name = _tunnelName.value,
                wgQuick = wgQuick
            )?.let {
                tunnelRepo.save(it)
                val settings = settingsRepo.getAll()
                if(settings != null) {
                    val setting = settings[0]
                    if(setting.defaultTunnel != null) {
                        if(it.id == TunnelConfig.from(setting.defaultTunnel!!).id) {
                            settingsRepo.save(setting.copy(
                                defaultTunnel = it.toString()
                            ))
                        }
                    }
                }
            }
        }
    }
}