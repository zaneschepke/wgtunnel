package com.zaneschepke.wireguardautotunnel.ui.screens.config

import android.Manifest
import android.app.Application
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wireguard.config.Config
import com.wireguard.config.Interface
import com.wireguard.config.Peer
import com.wireguard.crypto.Key
import com.wireguard.crypto.KeyPair
import com.zaneschepke.wireguardautotunnel.WireGuardAutoTunnel
import com.zaneschepke.wireguardautotunnel.data.model.TunnelConfig
import com.zaneschepke.wireguardautotunnel.data.repository.SettingsRepository
import com.zaneschepke.wireguardautotunnel.data.repository.TunnelConfigRepository
import com.zaneschepke.wireguardautotunnel.ui.models.InterfaceProxy
import com.zaneschepke.wireguardautotunnel.ui.models.PeerProxy
import com.zaneschepke.wireguardautotunnel.util.Constants
import com.zaneschepke.wireguardautotunnel.util.Event
import com.zaneschepke.wireguardautotunnel.util.NumberUtils
import com.zaneschepke.wireguardautotunnel.util.Result
import com.zaneschepke.wireguardautotunnel.util.removeAt
import com.zaneschepke.wireguardautotunnel.util.update
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ConfigViewModel
@Inject
constructor(
    private val application: Application,
    private val tunnelConfigRepository: TunnelConfigRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val packageManager = application.packageManager

    private val _uiState = MutableStateFlow(ConfigUiState())
    val uiState = _uiState.asStateFlow()

    fun init(tunnelId: String) =
        viewModelScope.launch(Dispatchers.IO) {
            val packages = getQueriedPackages("")
            val state =
                if (tunnelId != Constants.MANUAL_TUNNEL_CONFIG_ID) {
                    val tunnelConfig =
                        tunnelConfigRepository.getAll().firstOrNull { it.id.toString() == tunnelId }
                    if (tunnelConfig != null) {
                        val config = TunnelConfig.configFromQuick(tunnelConfig.wgQuick)
                        val proxyPeers = config.peers.map { PeerProxy.from(it) }
                        val proxyInterface = InterfaceProxy.from(config.`interface`)
                        var include = true
                        var isAllApplicationsEnabled = false
                        val checkedPackages =
                            if (config.`interface`.includedApplications.isNotEmpty()) {
                                config.`interface`.includedApplications
                            } else if (config.`interface`.excludedApplications.isNotEmpty()) {
                                include = false
                                config.`interface`.excludedApplications
                            } else {
                                isAllApplicationsEnabled = true
                                emptySet()
                            }
                        ConfigUiState(
                            proxyPeers,
                            proxyInterface,
                            packages,
                            checkedPackages.toList(),
                            include,
                            isAllApplicationsEnabled,
                            false,
                            tunnelConfig,
                            tunnelConfig.name,
                        )
                    } else {
                        ConfigUiState(loading = false, packages = packages)
                    }
                } else {
                    ConfigUiState(loading = false, packages = packages)
                }
            _uiState.value = state
        }

    fun onTunnelNameChange(name: String) {
        _uiState.value = _uiState.value.copy(tunnelName = name)
    }

    fun onIncludeChange(include: Boolean) {
        _uiState.value = _uiState.value.copy(include = include)
    }

    fun onAddCheckedPackage(packageName: String) {
        _uiState.value =
            _uiState.value.copy(
                checkedPackageNames = _uiState.value.checkedPackageNames + packageName
            )
    }

    fun onAllApplicationsChange(isAllApplicationsEnabled: Boolean) {
        _uiState.value = _uiState.value.copy(isAllApplicationsEnabled = isAllApplicationsEnabled)
    }

    fun onRemoveCheckedPackage(packageName: String) {
        _uiState.value =
            _uiState.value.copy(
                checkedPackageNames = _uiState.value.checkedPackageNames - packageName
            )
    }

    private fun getQueriedPackages(query: String): List<PackageInfo> {
        return getAllInternetCapablePackages().filter {
            getPackageLabel(it).lowercase().contains(query.lowercase())
        }
    }

    fun getPackageLabel(packageInfo: PackageInfo): String {
        return packageInfo.applicationInfo.loadLabel(application.packageManager).toString()
    }

    private fun getAllInternetCapablePackages(): List<PackageInfo> {
        return getPackagesHoldingPermissions(arrayOf(Manifest.permission.INTERNET))
    }

    private fun getPackagesHoldingPermissions(permissions: Array<String>): List<PackageInfo> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackagesHoldingPermissions(
                permissions,
                PackageManager.PackageInfoFlags.of(0L),
            )
        } else {
            packageManager.getPackagesHoldingPermissions(permissions, 0)
        }
    }

    private fun isAllApplicationsEnabled(): Boolean {
        return _uiState.value.isAllApplicationsEnabled
    }

    private fun saveConfig(tunnelConfig: TunnelConfig) =
        viewModelScope.launch { tunnelConfigRepository.save(tunnelConfig) }

    private fun updateTunnelConfig(tunnelConfig: TunnelConfig?) =
        viewModelScope.launch {
            if (tunnelConfig != null) {
                saveConfig(tunnelConfig).join()
                WireGuardAutoTunnel.requestTileServiceStateUpdate()
                updateSettingsDefaultTunnel(tunnelConfig)
            }
        }

    private suspend fun updateSettingsDefaultTunnel(tunnelConfig: TunnelConfig) {
        val settings = settingsRepository.getSettingsFlow().first()
        if (settings.defaultTunnel != null) {
            if (tunnelConfig.id == TunnelConfig.from(settings.defaultTunnel!!).id) {
                settingsRepository.save(settings.copy(defaultTunnel = tunnelConfig.toString()))
            }
        }
    }

    private fun buildPeerListFromProxyPeers(): List<Peer> {
        return _uiState.value.proxyPeers.map {
            val builder = Peer.Builder()
            if (it.allowedIps.isNotEmpty()) builder.parseAllowedIPs(it.allowedIps.trim())
            if (it.publicKey.isNotEmpty()) builder.parsePublicKey(it.publicKey.trim())
            if (it.preSharedKey.isNotEmpty()) builder.parsePreSharedKey(it.preSharedKey.trim())
            if (it.endpoint.isNotEmpty()) builder.parseEndpoint(it.endpoint.trim())
            if (it.persistentKeepalive.isNotEmpty()) {
                builder.parsePersistentKeepalive(it.persistentKeepalive.trim())
            }
            builder.build()
        }
    }

    private fun emptyCheckedPackagesList() {
        _uiState.value = _uiState.value.copy(checkedPackageNames = emptyList())
    }

    private fun buildInterfaceListFromProxyInterface(): Interface {
        val builder = Interface.Builder()
        builder.parsePrivateKey(_uiState.value.interfaceProxy.privateKey.trim())
        builder.parseAddresses(_uiState.value.interfaceProxy.addresses.trim())
        if (_uiState.value.interfaceProxy.dnsServers.isNotEmpty()) {
            builder.parseDnsServers(_uiState.value.interfaceProxy.dnsServers.trim())
        }
        if (_uiState.value.interfaceProxy.mtu.isNotEmpty())
            builder.parseMtu(_uiState.value.interfaceProxy.mtu.trim())
        if (_uiState.value.interfaceProxy.listenPort.isNotEmpty()) {
            builder.parseListenPort(_uiState.value.interfaceProxy.listenPort.trim())
        }
        if (isAllApplicationsEnabled()) emptyCheckedPackagesList()
        if (_uiState.value.include) builder.includeApplications(_uiState.value.checkedPackageNames)
        if (!_uiState.value.include) builder.excludeApplications(_uiState.value.checkedPackageNames)
        return builder.build()
    }

    fun onSaveAllChanges(): Result<Event> {
        return try {
            val peerList = buildPeerListFromProxyPeers()
            val wgInterface = buildInterfaceListFromProxyInterface()
            val config = Config.Builder().addPeers(peerList).setInterface(wgInterface).build()
            val tunnelConfig = when(uiState.value.tunnel) {
                null -> TunnelConfig(name = _uiState.value.tunnelName, wgQuick = config.toWgQuickString())
                else -> uiState.value.tunnel!!.copy(
                    name = _uiState.value.tunnelName,
                    wgQuick = config.toWgQuickString(),
                )
            }
            updateTunnelConfig(tunnelConfig)
            Result.Success(Event.Message.ConfigSaved)
        } catch (e: Exception) {
            Timber.e(e)
            Result.Error(Event.Error.Exception(e))
        }
    }

    fun onPeerPublicKeyChange(index: Int, value: String) {
        _uiState.value =
            _uiState.value.copy(
                proxyPeers =
                    _uiState.value.proxyPeers.update(
                        index,
                        _uiState.value.proxyPeers[index].copy(publicKey = value),
                    ),
            )
    }

    fun onPreSharedKeyChange(index: Int, value: String) {
        _uiState.value =
            _uiState.value.copy(
                proxyPeers =
                    _uiState.value.proxyPeers.update(
                        index,
                        _uiState.value.proxyPeers[index].copy(preSharedKey = value),
                    ),
            )
    }

    fun onEndpointChange(index: Int, value: String) {
        _uiState.value =
            _uiState.value.copy(
                proxyPeers =
                    _uiState.value.proxyPeers.update(
                        index,
                        _uiState.value.proxyPeers[index].copy(endpoint = value),
                    ),
            )
    }

    fun onAllowedIpsChange(index: Int, value: String) {
        _uiState.value =
            _uiState.value.copy(
                proxyPeers =
                    _uiState.value.proxyPeers.update(
                        index,
                        _uiState.value.proxyPeers[index].copy(allowedIps = value),
                    ),
            )
    }

    fun onPersistentKeepaliveChanged(index: Int, value: String) {
        _uiState.value =
            _uiState.value.copy(
                proxyPeers =
                    _uiState.value.proxyPeers.update(
                        index,
                        _uiState.value.proxyPeers[index].copy(persistentKeepalive = value),
                    ),
            )
    }

    fun onDeletePeer(index: Int) {
        _uiState.value =
            _uiState.value.copy(
                proxyPeers = _uiState.value.proxyPeers.removeAt(index),
            )
    }

    fun addEmptyPeer() {
        _uiState.value = _uiState.value.copy(proxyPeers = _uiState.value.proxyPeers + PeerProxy())
    }

    fun generateKeyPair() {
        val keyPair = KeyPair()
        _uiState.value =
            _uiState.value.copy(
                interfaceProxy =
                    _uiState.value.interfaceProxy.copy(
                        privateKey = keyPair.privateKey.toBase64(),
                        publicKey = keyPair.publicKey.toBase64(),
                    ),
            )
    }

    fun onAddressesChanged(value: String) {
        _uiState.value =
            _uiState.value.copy(
                interfaceProxy = _uiState.value.interfaceProxy.copy(addresses = value)
            )
    }

    fun onListenPortChanged(value: String) {
        _uiState.value =
            _uiState.value.copy(
                interfaceProxy = _uiState.value.interfaceProxy.copy(listenPort = value)
            )
    }

    fun onDnsServersChanged(value: String) {
        _uiState.value =
            _uiState.value.copy(
                interfaceProxy = _uiState.value.interfaceProxy.copy(dnsServers = value)
            )
    }

    fun onMtuChanged(value: String) {
        _uiState.value =
            _uiState.value.copy(interfaceProxy = _uiState.value.interfaceProxy.copy(mtu = value))
    }

    private fun onInterfacePublicKeyChange(value: String) {
        _uiState.value =
            _uiState.value.copy(
                interfaceProxy = _uiState.value.interfaceProxy.copy(publicKey = value)
            )
    }

    fun onPrivateKeyChange(value: String) {
        _uiState.value =
            _uiState.value.copy(
                interfaceProxy = _uiState.value.interfaceProxy.copy(privateKey = value)
            )
        if (NumberUtils.isValidKey(value)) {
            val pair = KeyPair(Key.fromBase64(value))
            onInterfacePublicKeyChange(pair.publicKey.toBase64())
        } else {
            onInterfacePublicKeyChange("")
        }
    }

    fun emitQueriedPackages(query: String) {
        val packages =
            getAllInternetCapablePackages().filter {
                getPackageLabel(it).lowercase().contains(query.lowercase())
            }
        _uiState.value = _uiState.value.copy(packages = packages)
    }
}
