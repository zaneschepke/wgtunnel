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
import com.wireguard.config.Interface
import com.wireguard.config.Peer
import com.wireguard.crypto.Key
import com.wireguard.crypto.KeyPair
import com.zaneschepke.wireguardautotunnel.Constants
import com.zaneschepke.wireguardautotunnel.repository.SettingsDoa
import com.zaneschepke.wireguardautotunnel.repository.TunnelConfigDao
import com.zaneschepke.wireguardautotunnel.repository.model.TunnelConfig
import com.zaneschepke.wireguardautotunnel.ui.models.InterfaceProxy
import com.zaneschepke.wireguardautotunnel.ui.models.PeerProxy
import com.zaneschepke.wireguardautotunnel.util.NumberUtils
import com.zaneschepke.wireguardautotunnel.util.WgTunnelException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ConfigViewModel @Inject constructor(private val application : Application,
                                          private val tunnelRepo : TunnelConfigDao,
                                          private val settingsRepo : SettingsDoa
) : ViewModel() {

    private val _tunnel = MutableStateFlow<TunnelConfig?>(null)
    private val _tunnelName = MutableStateFlow("")
    val tunnelName get() = _tunnelName.asStateFlow()
    val tunnel get() = _tunnel.asStateFlow()

    private var _proxyPeers = MutableStateFlow(mutableStateListOf<PeerProxy>())
    val proxyPeers get() = _proxyPeers.asStateFlow()

    private var _interface = MutableStateFlow(InterfaceProxy())
    val interfaceProxy = _interface.asStateFlow()

    private val _packages = MutableStateFlow(emptyList<PackageInfo>())
    val packages get() = _packages.asStateFlow()
    private val packageManager = application.packageManager

    private val _checkedPackages = MutableStateFlow(mutableStateListOf<String>())
    val checkedPackages get() = _checkedPackages.asStateFlow()
    private val _include = MutableStateFlow(true)
    val include get() = _include.asStateFlow()

    private val _isAllApplicationsEnabled = MutableStateFlow(false)
    val isAllApplicationsEnabled get() = _isAllApplicationsEnabled.asStateFlow()
    private val _isDefaultTunnel = MutableStateFlow(false)
    val isDefaultTunnel = _isDefaultTunnel.asStateFlow()

    private lateinit var tunnelConfig: TunnelConfig

    fun onScreenLoad(id : String) {
        if(id != Constants.MANUAL_TUNNEL_CONFIG_ID) {
            viewModelScope.launch(Dispatchers.IO) {
                tunnelConfig = withContext(this.coroutineContext) {
                    getTunnelConfigById(id) ?: throw WgTunnelException("Config not found")
                }
                emitScreenData()
            }
        } else {
            emitEmptyScreenData()
        }
    }

    private fun emitEmptyScreenData() {
        tunnelConfig = TunnelConfig(name = NumberUtils.generateRandomTunnelName(), wgQuick = "")
        viewModelScope.launch {
            emitTunnelConfig()
            emitPeerProxy(PeerProxy())
            emitInterfaceProxy(InterfaceProxy())
            emitTunnelConfigName()
            emitDefaultTunnelStatus()
            emitQueriedPackages("")
            emitTunnelAllApplicationsEnabled()
        }
    }


    private suspend fun emitScreenData() {
        emitTunnelConfig()
        emitPeersFromConfig()
        emitInterfaceFromConfig()
        emitTunnelConfigName()
        emitDefaultTunnelStatus()
        emitQueriedPackages("")
        emitCurrentPackageConfigurations()
    }

    private suspend fun emitDefaultTunnelStatus() {
        val settings = settingsRepo.getAll()
        if(settings.isNotEmpty()) {
            _isDefaultTunnel.value = settings.first().isTunnelConfigDefault(tunnelConfig)
        }
    }

    private fun emitInterfaceFromConfig() {
        val config = TunnelConfig.configFromQuick(tunnelConfig.wgQuick)
        _interface.value = InterfaceProxy.from(config.`interface`)
    }

    private fun emitPeersFromConfig() {
        val config = TunnelConfig.configFromQuick(tunnelConfig.wgQuick)
        config.peers.forEach{
            _proxyPeers.value.add(PeerProxy.from(it))
        }
    }

    private fun emitPeerProxy(peerProxy: PeerProxy) {
        _proxyPeers.value.add(peerProxy)
    }

    private fun emitInterfaceProxy(interfaceProxy: InterfaceProxy) {
        _interface.value = interfaceProxy
    }

    private suspend fun getTunnelConfigById(id : String) : TunnelConfig? {
        return try {
            tunnelRepo.getById(id.toLong())
        } catch (_ : Exception) {
            null
        }
    }

    private suspend fun emitTunnelConfig() {
        _tunnel.emit(tunnelConfig)
    }

    private  suspend fun emitTunnelConfigName() {
        _tunnelName.emit(tunnelConfig.name)
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

    fun onAllApplicationsChange(isAllApplicationsEnabled : Boolean) {
        _isAllApplicationsEnabled.value = isAllApplicationsEnabled
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
        _isAllApplicationsEnabled.emit(true)
    }

    private suspend fun emitTunnelAllApplicationsDisabled() {
        _isAllApplicationsEnabled.emit(false)
    }

    private fun emitCurrentPackageConfigurations() {
        viewModelScope.launch(Dispatchers.IO) {
            val config = TunnelConfig.configFromQuick(tunnelConfig.wgQuick)
            emitSplitTunnelConfiguration(config)
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

    private fun isAllApplicationsEnabled() : Boolean {
        return _isAllApplicationsEnabled.value
    }

    private fun isIncludeApplicationsEnabled() : Boolean {
        return _include.value
    }

    private suspend fun saveConfig(tunnelConfig: TunnelConfig) {
        tunnelRepo.save(tunnelConfig)
    }
    private suspend fun updateTunnelConfig(tunnelConfig: TunnelConfig?) {
        if(tunnelConfig != null) {
            saveConfig(tunnelConfig)
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

    fun buildPeerListFromProxyPeers() : List<Peer> {
        return _proxyPeers.value.map {
            val builder = Peer.Builder()
            if (it.allowedIps.isNotEmpty()) builder.parseAllowedIPs(it.allowedIps.trim())
            if (it.publicKey.isNotEmpty()) builder.parsePublicKey(it.publicKey.trim())
            if (it.preSharedKey.isNotEmpty()) builder.parsePreSharedKey(it.preSharedKey.trim())
            if (it.endpoint.isNotEmpty()) builder.parseEndpoint(it.endpoint.trim())
            if (it.persistentKeepalive.isNotEmpty()) builder.parsePersistentKeepalive(it.persistentKeepalive.trim())
            builder.build()
        }
    }

    fun buildInterfaceListFromProxyInterface() : Interface {
        val builder = Interface.Builder()
        builder.parsePrivateKey(_interface.value.privateKey.trim())
        builder.parseAddresses(_interface.value.addresses.trim())
        builder.parseDnsServers(_interface.value.dnsServers.trim())
        if(_interface.value.mtu.isNotEmpty()) builder.parseMtu(_interface.value.mtu.trim())
        if(_interface.value.listenPort.isNotEmpty()) builder.parseListenPort(_interface.value.listenPort.trim())
        if(isAllApplicationsEnabled()) _checkedPackages.value.clear()
        if(_include.value) builder.includeApplications(_checkedPackages.value)
        if(!_include.value) builder.excludeApplications(_checkedPackages.value)
        return builder.build()
    }



    suspend fun onSaveAllChanges() {
        try {
            val peerList = buildPeerListFromProxyPeers()
            val wgInterface = buildInterfaceListFromProxyInterface()
            val config = Config.Builder().addPeers(peerList).setInterface(wgInterface).build()
            val tunnelConfig = _tunnel.value?.copy(
                name = _tunnelName.value,
                wgQuick = config.toWgQuickString()
            )
            updateTunnelConfig(tunnelConfig)
        } catch (e : Exception) {
            throw WgTunnelException("Error: ${e.cause?.message?.lowercase() ?: "unknown error occurred"}")
        }
    }

    fun onPeerPublicKeyChange(index: Int, publicKey: String) {
        _proxyPeers.value[index] = _proxyPeers.value[index].copy(
            publicKey = publicKey
        )
    }

    fun onPreSharedKeyChange(index: Int, value: String) {
        _proxyPeers.value[index] = _proxyPeers.value[index].copy(
            preSharedKey = value
        )
    }

    fun onEndpointChange(index: Int, value: String) {
        _proxyPeers.value[index] = _proxyPeers.value[index].copy(
            endpoint = value
        )
    }

    fun onAllowedIpsChange(index: Int, value: String) {
        _proxyPeers.value[index] = _proxyPeers.value[index].copy(
            allowedIps = value
        )
    }

    fun onPersistentKeepaliveChanged(index : Int, value : String) {
        _proxyPeers.value[index] = _proxyPeers.value[index].copy(
            persistentKeepalive = value
        )
    }

    fun onDeletePeer(index: Int) {
        proxyPeers.value.removeAt(index)
    }

    fun addEmptyPeer() {
        _proxyPeers.value.add(PeerProxy())
    }

    fun generateKeyPair() {
        val keyPair = KeyPair()
        _interface.value = _interface.value.copy(
            privateKey = keyPair.privateKey.toBase64(),
            publicKey = keyPair.publicKey.toBase64()
        )
    }

    fun onAddressesChanged(value: String) {
        _interface.value = _interface.value.copy(
            addresses = value
        )
    }

    fun onListenPortChanged(value: String) {
        _interface.value = _interface.value.copy(
            listenPort = value
        )
    }

    fun onDnsServersChanged(value: String) {
        _interface.value = _interface.value.copy(
            dnsServers = value
        )
    }

    fun onMtuChanged(value: String) {
        _interface.value = _interface.value.copy(
            mtu = value
        )
    }

    private fun onInterfacePublicKeyChange(value : String) {
        _interface.value = _interface.value.copy(
            publicKey = value
        )
    }

    fun onPrivateKeyChange(value: String) {
        _interface.value = _interface.value.copy(
            privateKey = value
        )
        if(NumberUtils.isValidKey(value)) {
            val pair = KeyPair(Key.fromBase64(value))
            onInterfacePublicKeyChange(pair.publicKey.toBase64())
        } else {
            onInterfacePublicKeyChange("")
        }
    }
}