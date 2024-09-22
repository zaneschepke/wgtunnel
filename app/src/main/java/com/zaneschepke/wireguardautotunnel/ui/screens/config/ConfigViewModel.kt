package com.zaneschepke.wireguardautotunnel.ui.screens.config

import android.Manifest
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import com.wireguard.config.Config
import com.wireguard.config.Interface
import com.wireguard.config.Peer
import com.wireguard.crypto.Key
import com.wireguard.crypto.KeyPair
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.WireGuardAutoTunnel
import com.zaneschepke.wireguardautotunnel.data.domain.TunnelConfig
import com.zaneschepke.wireguardautotunnel.data.repository.AppDataRepository
import com.zaneschepke.wireguardautotunnel.module.IoDispatcher
import com.zaneschepke.wireguardautotunnel.ui.Screens
import com.zaneschepke.wireguardautotunnel.ui.common.snackbar.SnackbarController
import com.zaneschepke.wireguardautotunnel.ui.screens.config.model.PeerProxy
import com.zaneschepke.wireguardautotunnel.util.Constants
import com.zaneschepke.wireguardautotunnel.util.NumberUtils
import com.zaneschepke.wireguardautotunnel.util.StringValue
import com.zaneschepke.wireguardautotunnel.util.extensions.removeAt
import com.zaneschepke.wireguardautotunnel.util.extensions.update
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import timber.log.Timber

@HiltViewModel(assistedFactory = ConfigViewModel.ConfigViewModelFactory::class)
class ConfigViewModel
@AssistedInject
constructor(
	private val appDataRepository: AppDataRepository,
	private val navController: NavHostController,
	@Assisted val id: Int,
	@IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
	private val packageManager = WireGuardAutoTunnel.instance.packageManager

	private val _uiState = MutableStateFlow(ConfigUiState())
	val uiState = _uiState.onStart {
		appDataRepository.tunnels.getById(id)?.let {
			val packages = getQueriedPackages()
			_uiState.value = ConfigUiState.from(it).copy(
				packages = packages,
			)
		}
	}.stateIn(
		viewModelScope + ioDispatcher,
		SharingStarted.WhileSubscribed(Constants.SUBSCRIPTION_TIMEOUT),
		ConfigUiState(),
	)

	fun onTunnelNameChange(name: String) {
		_uiState.update {
			it.copy(tunnelName = name)
		}
	}

	fun onIncludeChange(include: Boolean) {
		_uiState.update {
			it.copy(include = include)
		}
	}

	fun cleanUpUninstalledApps() = viewModelScope.launch(ioDispatcher) {
		uiState.value.tunnel?.let {
			val config = it.toAmConfig()
			val packages = getQueriedPackages()
			val packageSet = packages.map { pack -> pack.packageName }.toSet()
			val includedApps = config.`interface`.includedApplications.toMutableList()
			val excludedApps = config.`interface`.excludedApplications.toMutableList()
			if (includedApps.isEmpty() && excludedApps.isEmpty()) return@launch
			if (includedApps.retainAll(packageSet) || excludedApps.retainAll(packageSet)) {
				Timber.i("Removing split tunnel package name that no longer exists on the device")
				_uiState.update { state ->
					state.copy(
						checkedPackageNames = if (_uiState.value.include) includedApps else excludedApps,
					)
				}
				val wgQuick = buildConfig().toWgQuickString(true)
				val amQuick = buildAmConfig().toAwgQuickString(true)
				saveConfig(
					it.copy(
						amQuick = amQuick,
						wgQuick = wgQuick,
					),
				)
			}
		}
	}

	fun onAddCheckedPackage(packageName: String) {
		_uiState.update {
			it.copy(
				checkedPackageNames = it.checkedPackageNames + packageName,
			)
		}
	}

	fun onAllApplicationsChange(isAllApplicationsEnabled: Boolean) {
		_uiState.update {
			it.copy(isAllApplicationsEnabled = isAllApplicationsEnabled)
		}
	}

	fun onRemoveCheckedPackage(packageName: String) {
		_uiState.update {
			it.copy(
				checkedPackageNames = it.checkedPackageNames - packageName,
			)
		}
	}

	private fun getQueriedPackages(query: String = ""): List<PackageInfo> {
		return getAllInternetCapablePackages().filter {
			getPackageLabel(it).lowercase().contains(query.lowercase())
		}
	}

	fun getPackageLabel(packageInfo: PackageInfo): String {
		return packageInfo.applicationInfo?.loadLabel(packageManager).toString()
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

	private fun saveConfig(tunnelConfig: TunnelConfig) = viewModelScope.launch {
		appDataRepository.tunnels.save(tunnelConfig)
	}

	private fun updateTunnelConfig(tunnelConfig: TunnelConfig?) = viewModelScope.launch {
		if (tunnelConfig != null) {
			saveConfig(tunnelConfig).join()
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
		_uiState.update {
			it.copy(checkedPackageNames = emptyList())
		}
	}

	private fun buildInterfaceListFromProxyInterface(): Interface {
		val builder = Interface.Builder()
		with(_uiState.value.interfaceProxy) {
			builder.parsePrivateKey(this.privateKey.trim())
			builder.parseAddresses(this.addresses.trim())
			if (this.dnsServers.isNotEmpty()) {
				builder.parseDnsServers(this.dnsServers.trim())
			}
			if (this.mtu.isNotEmpty()) {
				builder.parseMtu(this.mtu.trim())
			}
			if (this.listenPort.isNotEmpty()) {
				builder.parseListenPort(this.listenPort.trim())
			}
			if (isAllApplicationsEnabled()) emptyCheckedPackagesList()
			if (_uiState.value.include) {
				builder.includeApplications(
					_uiState.value.checkedPackageNames,
				)
			}
			if (!_uiState.value.include) {
				builder.excludeApplications(
					_uiState.value.checkedPackageNames,
				)
			}
		}

		return builder.build()
	}

	private fun buildAmInterfaceListFromProxyInterface(): org.amnezia.awg.config.Interface {
		val builder = org.amnezia.awg.config.Interface.Builder()
		with(_uiState.value.interfaceProxy) {
			builder.parsePrivateKey(this.privateKey.trim())
			builder.parseAddresses(this.addresses.trim())
			if (this.dnsServers.isNotEmpty()) {
				builder.parseDnsServers(this.dnsServers.trim())
			}
			if (this.mtu.isNotEmpty()) {
				builder.parseMtu(this.mtu.trim())
			}
			if (this.listenPort.isNotEmpty()) {
				builder.parseListenPort(this.listenPort.trim())
			}
			if (isAllApplicationsEnabled()) emptyCheckedPackagesList()
			if (_uiState.value.include) {
				builder.includeApplications(
					_uiState.value.checkedPackageNames,
				)
			}
			if (!_uiState.value.include) {
				builder.excludeApplications(
					_uiState.value.checkedPackageNames,
				)
			}
			if (this.junkPacketCount.isNotEmpty()) {
				builder.setJunkPacketCount(
					this.junkPacketCount.trim().toInt(),
				)
			}
			if (this.junkPacketMinSize.isNotEmpty()) {
				builder.setJunkPacketMinSize(
					this.junkPacketMinSize.trim().toInt(),
				)
			}
			if (this.junkPacketMaxSize.isNotEmpty()) {
				builder.setJunkPacketMaxSize(
					this.junkPacketMaxSize.trim().toInt(),
				)
			}
			if (this.initPacketJunkSize.isNotEmpty()) {
				builder.setInitPacketJunkSize(
					this.initPacketJunkSize.trim().toInt(),
				)
			}
			if (this.responsePacketJunkSize.isNotEmpty()) {
				builder.setResponsePacketJunkSize(
					this.responsePacketJunkSize.trim().toInt(),
				)
			}
			if (this.initPacketMagicHeader.isNotEmpty()) {
				builder.setInitPacketMagicHeader(
					this.initPacketMagicHeader.trim().toLong(),
				)
			}
			if (this.responsePacketMagicHeader.isNotEmpty()) {
				builder.setResponsePacketMagicHeader(
					this.responsePacketMagicHeader.trim().toLong(),
				)
			}
			if (this.transportPacketMagicHeader.isNotEmpty()) {
				builder.setTransportPacketMagicHeader(
					this.transportPacketMagicHeader.trim().toLong(),
				)
			}
			if (this.underloadPacketMagicHeader.isNotEmpty()) {
				builder.setUnderloadPacketMagicHeader(
					this.underloadPacketMagicHeader.trim().toLong(),
				)
			}
		}

		return builder.build()
	}

	private fun buildConfig(): Config {
		val peerList = buildPeerListFromProxyPeers()
		val wgInterface = buildInterfaceListFromProxyInterface()
		return Config.Builder().addPeers(peerList).setInterface(wgInterface).build()
	}

	private fun buildAmConfig(): org.amnezia.awg.config.Config {
		val peerList = buildAmPeerListFromProxyPeers()
		val amInterface = buildAmInterfaceListFromProxyInterface()
		return org.amnezia.awg.config.Config.Builder().addPeers(
			peerList,
		).setInterface(amInterface)
			.build()
	}

	fun onSaveAllChanges() = viewModelScope.launch {
		kotlin.runCatching {
			val wgQuick = buildConfig().toWgQuickString(true)
			val amQuick = buildAmConfig().toAwgQuickString(true)
			val tunnelConfig = uiState.value.tunnel?.copy(
				name = _uiState.value.tunnelName,
				amQuick = amQuick,
				wgQuick = wgQuick,
			) ?: TunnelConfig(
				name = _uiState.value.tunnelName,
				wgQuick = wgQuick,
				amQuick = amQuick,
			)
			updateTunnelConfig(tunnelConfig)
			SnackbarController.showMessage(
				StringValue.StringResource(R.string.config_changes_saved),
			)
			navController.navigate(Screens.Main)
		}.onFailure {
			Timber.e(it)
			val message = it.message?.substringAfter(":", missingDelimiterValue = "")
			val stringValue =
				message?.let {
					StringValue.DynamicString(message)
				} ?: StringValue.StringResource(R.string.unknown_error)
			SnackbarController.showMessage(stringValue)
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
				it.interfaceProxy.copy(
					privateKey = keyPair.privateKey.toBase64(),
					publicKey = keyPair.publicKey.toBase64(),
				),
			)
		}
	}

	fun onAddressesChanged(value: String) {
		_uiState.update {
			it.copy(
				interfaceProxy = it.interfaceProxy.copy(addresses = value),
			)
		}
	}

	fun onListenPortChanged(value: String) {
		_uiState.update {
			it.copy(
				interfaceProxy = it.interfaceProxy.copy(listenPort = value),
			)
		}
	}

	fun onDnsServersChanged(value: String) {
		_uiState.update {
			it.copy(
				interfaceProxy = it.interfaceProxy.copy(dnsServers = value),
			)
		}
	}

	fun onMtuChanged(value: String) {
		_uiState.update {
			it.copy(interfaceProxy = it.interfaceProxy.copy(mtu = value))
		}
	}

	private fun onInterfacePublicKeyChange(value: String) {
		_uiState.update {
			it.copy(
				interfaceProxy = it.interfaceProxy.copy(publicKey = value),
			)
		}
	}

	fun onPrivateKeyChange(value: String) {
		_uiState.update {
			it.copy(
				interfaceProxy = it.interfaceProxy.copy(privateKey = value),
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
				interfaceProxy = it.interfaceProxy.copy(junkPacketCount = value),
			)
		}
	}

	fun onJunkPacketMinSizeChanged(value: String) {
		_uiState.update {
			it.copy(
				interfaceProxy = it.interfaceProxy.copy(junkPacketMinSize = value),
			)
		}
	}

	fun onJunkPacketMaxSizeChanged(value: String) {
		_uiState.update {
			it.copy(
				interfaceProxy = it.interfaceProxy.copy(junkPacketMaxSize = value),
			)
		}
	}

	fun onInitPacketJunkSizeChanged(value: String) {
		_uiState.update {
			it.copy(
				interfaceProxy = it.interfaceProxy.copy(initPacketJunkSize = value),
			)
		}
	}

	fun onResponsePacketJunkSize(value: String) {
		_uiState.update {
			it.copy(
				interfaceProxy =
				it.interfaceProxy.copy(
					responsePacketJunkSize = value,
				),
			)
		}
	}

	fun onInitPacketMagicHeader(value: String) {
		_uiState.update {
			it.copy(
				interfaceProxy =
				it.interfaceProxy.copy(
					initPacketMagicHeader = value,
				),
			)
		}
	}

	fun onResponsePacketMagicHeader(value: String) {
		_uiState.update {
			it.copy(
				interfaceProxy =
				it.interfaceProxy.copy(
					responsePacketMagicHeader = value,
				),
			)
		}
	}

	fun onTransportPacketMagicHeader(value: String) {
		_uiState.update {
			it.copy(
				interfaceProxy =
				it.interfaceProxy.copy(
					transportPacketMagicHeader = value,
				),
			)
		}
	}

	fun onUnderloadPacketMagicHeader(value: String) {
		_uiState.update {
			it.copy(
				interfaceProxy =
				it.interfaceProxy.copy(
					underloadPacketMagicHeader = value,
				),
			)
		}
	}

	@AssistedFactory
	interface ConfigViewModelFactory {
		fun create(id: Int): ConfigViewModel
	}
}
