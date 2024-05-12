package com.zaneschepke.wireguardautotunnel.ui.screens.config

import android.Manifest
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
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.WireGuardAutoTunnel
import com.zaneschepke.wireguardautotunnel.data.domain.TunnelConfig
import com.zaneschepke.wireguardautotunnel.data.repository.AppDataRepository
import com.zaneschepke.wireguardautotunnel.data.repository.SettingsRepository
import com.zaneschepke.wireguardautotunnel.ui.screens.config.model.PeerProxy
import com.zaneschepke.wireguardautotunnel.ui.screens.main.ConfigType
import com.zaneschepke.wireguardautotunnel.util.Constants
import com.zaneschepke.wireguardautotunnel.util.NumberUtils
import com.zaneschepke.wireguardautotunnel.util.StringValue
import com.zaneschepke.wireguardautotunnel.util.WgTunnelExceptions
import com.zaneschepke.wireguardautotunnel.util.removeAt
import com.zaneschepke.wireguardautotunnel.util.update
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ConfigViewModel
@Inject
constructor(
    private val settingsRepository: SettingsRepository,
    private val appDataRepository: AppDataRepository
) : ViewModel() {

    private val packageManager = WireGuardAutoTunnel.instance.packageManager

    private val _uiState = MutableStateFlow(ConfigUiState())
    val uiState = _uiState.asStateFlow()

    fun init(tunnelId: String) =
        viewModelScope.launch(Dispatchers.IO) {
            val packages = getQueriedPackages("")
            val state =
                if (tunnelId != Constants.MANUAL_TUNNEL_CONFIG_ID) {
                    val tunnelConfig =
                        appDataRepository.tunnels.getAll()
                            .firstOrNull { it.id.toString() == tunnelId }
                    val isAmneziaEnabled = settingsRepository.getSettings().isAmneziaEnabled
                    if (tunnelConfig != null) {
                        (if(isAmneziaEnabled) {
                            val amConfig = if(tunnelConfig.amQuick == "") tunnelConfig.wgQuick else tunnelConfig.amQuick
                            ConfigUiState.from(TunnelConfig.configFromAmQuick(amConfig))
                        } else ConfigUiState.from(TunnelConfig.configFromWgQuick(tunnelConfig.wgQuick))).copy(
                            packages = packages,
                            loading = false,
                            tunnel = tunnelConfig,
                            tunnelName = tunnelConfig.name,
                            isAmneziaEnabled = isAmneziaEnabled
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
                checkedPackageNames = _uiState.value.checkedPackageNames + packageName,
            )
    }

    fun onAllApplicationsChange(isAllApplicationsEnabled: Boolean) {
        _uiState.value = _uiState.value.copy(isAllApplicationsEnabled = isAllApplicationsEnabled)
    }

    fun onRemoveCheckedPackage(packageName: String) {
        _uiState.value =
            _uiState.value.copy(
                checkedPackageNames = _uiState.value.checkedPackageNames - packageName,
            )
    }

    private fun getQueriedPackages(query: String): List<PackageInfo> {
        return getAllInternetCapablePackages().filter {
            getPackageLabel(it).lowercase().contains(query.lowercase())
        }
    }

    fun getPackageLabel(packageInfo: PackageInfo): String {
        return packageInfo.applicationInfo.loadLabel(packageManager).toString()
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
        viewModelScope.launch { appDataRepository.tunnels.save(tunnelConfig) }

    private fun updateTunnelConfig(tunnelConfig: TunnelConfig?) =
        viewModelScope.launch {
            if (tunnelConfig != null) {
                saveConfig(tunnelConfig).join()
                WireGuardAutoTunnel.requestTunnelTileServiceStateUpdate()
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

    private fun buildAmPeerListFromProxyPeers(): List<org.amnezia.awg.config.Peer> {
        return _uiState.value.proxyPeers.map {
            val builder = org.amnezia.awg.config.Peer.Builder()
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

    private fun buildAmInterfaceListFromProxyInterface(): org.amnezia.awg.config.Interface {
        val builder = org.amnezia.awg.config.Interface.Builder()
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
        if(_uiState.value.interfaceProxy.junkPacketCount.isNotEmpty()) {
            builder.setJunkPacketCount(_uiState.value.interfaceProxy.junkPacketCount.trim().toInt())
        }
        if(_uiState.value.interfaceProxy.junkPacketMinSize.isNotEmpty()) {
            builder.setJunkPacketMinSize(_uiState.value.interfaceProxy.junkPacketMinSize.trim().toInt())
        }
        if(_uiState.value.interfaceProxy.junkPacketMaxSize.isNotEmpty()) {
            builder.setJunkPacketMaxSize(_uiState.value.interfaceProxy.junkPacketMaxSize.trim().toInt())
        }
        if(_uiState.value.interfaceProxy.initPacketJunkSize.isNotEmpty()) {
            builder.setInitPacketJunkSize(_uiState.value.interfaceProxy.initPacketJunkSize.trim().toInt())
        }
        if(_uiState.value.interfaceProxy.responsePacketJunkSize.isNotEmpty()) {
            builder.setResponsePacketJunkSize(_uiState.value.interfaceProxy.responsePacketJunkSize.trim().toInt())
        }
        if(_uiState.value.interfaceProxy.initPacketMagicHeader.isNotEmpty()) {
            builder.setInitPacketMagicHeader(_uiState.value.interfaceProxy.initPacketMagicHeader.trim().toLong())
        }
        if(_uiState.value.interfaceProxy.responsePacketMagicHeader.isNotEmpty()) {
            builder.setResponsePacketMagicHeader(_uiState.value.interfaceProxy.responsePacketMagicHeader.trim().toLong())
        }
        if(_uiState.value.interfaceProxy.transportPacketMagicHeader.isNotEmpty()) {
            builder.setTransportPacketMagicHeader(_uiState.value.interfaceProxy.transportPacketMagicHeader.trim().toLong())
        }
        if(_uiState.value.interfaceProxy.underloadPacketMagicHeader.isNotEmpty()) {
            builder.setUnderloadPacketMagicHeader(_uiState.value.interfaceProxy.underloadPacketMagicHeader.trim().toLong())
        }
        return builder.build()
    }

    private fun buildConfig() : Config {
        val peerList = buildPeerListFromProxyPeers()
        val wgInterface = buildInterfaceListFromProxyInterface()
        return  Config.Builder().addPeers(peerList).setInterface(wgInterface).build()
    }

    private fun buildAmConfig() : org.amnezia.awg.config.Config {
        val peerList = buildAmPeerListFromProxyPeers()
        val amInterface = buildAmInterfaceListFromProxyInterface()
        return org.amnezia.awg.config.Config.Builder().addPeers(peerList).setInterface(amInterface).build()
    }

    fun onSaveAllChanges(configType: ConfigType): Result<Unit> {
        return try {
            val wgQuick = buildConfig().toWgQuickString()
            val amQuick = if(configType == ConfigType.AMNEZIA) {
                buildAmConfig().toAwgQuickString()
            } else TunnelConfig.AM_QUICK_DEFAULT
            val tunnelConfig = when (uiState.value.tunnel) {
                null -> TunnelConfig(
                    name = _uiState.value.tunnelName,
                    wgQuick = wgQuick,
                    amQuick = amQuick
                )
                else -> uiState.value.tunnel!!.copy(
                    name = _uiState.value.tunnelName,
                    wgQuick = wgQuick,
                    amQuick = amQuick
                )
            }
            updateTunnelConfig(tunnelConfig)
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e)
            val message = e.message?.substringAfter(":", missingDelimiterValue = "")
            val stringValue = message?.let {
                StringValue.DynamicString(message)
            } ?: StringValue.StringResource(R.string.unknown_error)
            Result.failure(WgTunnelExceptions.ConfigParseError(stringValue))
        }
    }

    fun onPeerPublicKeyChange(index: Int, value: String) {
        _uiState.update {
            it.copy(
                proxyPeers =
                _uiState.value.proxyPeers.update(
                    index,
                    _uiState.value.proxyPeers[index].copy(publicKey = value),
                ),
            )
        }
    }

    fun onPreSharedKeyChange(index: Int, value: String) {
        _uiState.update {
            it.copy(
                proxyPeers =
                _uiState.value.proxyPeers.update(
                    index,
                    _uiState.value.proxyPeers[index].copy(preSharedKey = value),
                ),
            )
        }
    }

    fun onEndpointChange(index: Int, value: String) {
        _uiState.update {
            it.copy(
                proxyPeers =
                _uiState.value.proxyPeers.update(
                    index,
                    _uiState.value.proxyPeers[index].copy(endpoint = value),
                ),
            )
        }
    }

    fun onAllowedIpsChange(index: Int, value: String) {
        _uiState.update {
            it.copy(
                proxyPeers =
                _uiState.value.proxyPeers.update(
                    index,
                    _uiState.value.proxyPeers[index].copy(allowedIps = value),
                ),
            )
        }
    }

    fun onPersistentKeepaliveChanged(index: Int, value: String) {
        _uiState.update {
            it.copy(
                proxyPeers =
                _uiState.value.proxyPeers.update(
                    index,
                    _uiState.value.proxyPeers[index].copy(persistentKeepalive = value),
                ),
            )
        }
    }

    fun onDeletePeer(index: Int) {
        _uiState.update {
            it.copy(
                proxyPeers = _uiState.value.proxyPeers.removeAt(index),
            )
        }
    }

    fun addEmptyPeer() {
        _uiState.update {
            it.copy(proxyPeers = _uiState.value.proxyPeers + PeerProxy())
        }
    }

    fun generateKeyPair() {
        val keyPair = KeyPair()
        _uiState.update {
            it.copy(
                interfaceProxy =
                _uiState.value.interfaceProxy.copy(
                    privateKey = keyPair.privateKey.toBase64(),
                    publicKey = keyPair.publicKey.toBase64(),
                ),
            )
        }
    }

    fun onAddressesChanged(value: String) {
        _uiState.update {
            it.copy(
                interfaceProxy = _uiState.value.interfaceProxy.copy(addresses = value),
            )
        }

    }

    fun onListenPortChanged(value: String) {
        _uiState.update {
            it.copy(
                interfaceProxy = _uiState.value.interfaceProxy.copy(listenPort = value),
            )
        }
    }

    fun onDnsServersChanged(value: String) {
        _uiState.update {
            it.copy(
                interfaceProxy = _uiState.value.interfaceProxy.copy(dnsServers = value),
            )
        }
    }

    fun onMtuChanged(value: String) {
        _uiState.update {
            it.copy(interfaceProxy = _uiState.value.interfaceProxy.copy(mtu = value))
        }
    }

    private fun onInterfacePublicKeyChange(value: String) {
        _uiState.update {
            it.copy(
                interfaceProxy = _uiState.value.interfaceProxy.copy(publicKey = value),
            )
        }

    }

    fun onPrivateKeyChange(value: String) {
        _uiState.update {
            it.copy(
                interfaceProxy = _uiState.value.interfaceProxy.copy(privateKey = value),
            )
        }
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
        _uiState.update { it.copy(packages = packages) }
    }

    fun onJunkPacketCountChanged(value: String) {
        _uiState.update {
            it.copy(
                interfaceProxy = _uiState.value.interfaceProxy.copy(junkPacketCount = value)
            )
        }
    }
    fun onJunkPacketMinSizeChanged(value: String) {
        _uiState.update {
            it.copy(
                interfaceProxy = _uiState.value.interfaceProxy.copy(junkPacketMinSize = value)
            )
        }
    }

    fun onJunkPacketMaxSizeChanged(value: String) {
        _uiState.update {
            it.copy(
                interfaceProxy = _uiState.value.interfaceProxy.copy(junkPacketMaxSize = value)
            )
        }
    }

    fun onInitPacketJunkSizeChanged(value: String) {
        _uiState.update {
            it.copy(
                interfaceProxy = _uiState.value.interfaceProxy.copy(initPacketJunkSize = value)
            )
        }
    }

    fun onResponsePacketJunkSize(value: String) {
        _uiState.update {
            it.copy(
                interfaceProxy = _uiState.value.interfaceProxy.copy(responsePacketJunkSize = value)
            )
        }
    }

    fun onInitPacketMagicHeader(value: String) {
        _uiState.update {
            it.copy(
                interfaceProxy = _uiState.value.interfaceProxy.copy(initPacketMagicHeader = value)
            )
        }
    }

    fun onResponsePacketMagicHeader(value: String) {
        _uiState.update {
            it.copy(
                interfaceProxy = _uiState.value.interfaceProxy.copy(responsePacketMagicHeader = value)
            )
        }
    }

    fun onTransportPacketMagicHeader(value: String) {
        _uiState.update {
            it.copy(
                interfaceProxy = _uiState.value.interfaceProxy.copy(transportPacketMagicHeader = value)
            )
        }
    }

    fun onUnderloadPacketMagicHeader(value: String) {
        _uiState.update {
            it.copy(
                interfaceProxy = _uiState.value.interfaceProxy.copy(underloadPacketMagicHeader = value)
            )
        }
    }
}
