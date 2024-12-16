package com.zaneschepke.wireguardautotunnel.ui.screens.tunneloptions.splittunnel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wireguard.config.Config
import com.wireguard.config.Interface
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.data.domain.TunnelConfig
import com.zaneschepke.wireguardautotunnel.data.repository.AppDataRepository
import com.zaneschepke.wireguardautotunnel.module.IoDispatcher
import com.zaneschepke.wireguardautotunnel.ui.common.snackbar.SnackbarController
import com.zaneschepke.wireguardautotunnel.util.StringValue
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplitTunnelViewModel
@Inject constructor(
	private val appDataRepository: AppDataRepository,
	@IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

	private val _saved = MutableSharedFlow<Boolean>()
	val saved = _saved.asSharedFlow()

	fun saveSplitTunnelConfig(included: Collection<String>, excluded: Collection<String>, tunnelConfig: TunnelConfig) = viewModelScope.launch(
		ioDispatcher,
	) {
		runCatching {
			val amConfig = tunnelConfig.toAmConfig()
			val wgConfig = tunnelConfig.toWgConfig()

			appDataRepository.tunnels.save(
				tunnelConfig.copy(
					wgQuick = Config.Builder().apply {
						setInterface(rebuildWgInterface(included, excluded, wgConfig))
						addPeers(wgConfig.peers)
					}.build().toWgQuickString(true),
					amQuick = org.amnezia.awg.config.Config.Builder().apply {
						setInterface(rebuildAmInterface(included, excluded, amConfig))
						addPeers(amConfig.peers)
					}.build().toAwgQuickString(true),
				),
			)
			SnackbarController.showMessage(StringValue.StringResource(R.string.config_changes_saved))
			_saved.emit(true)
		}.onFailure {
			// TODO improve errors
			SnackbarController.showMessage(StringValue.StringResource(R.string.unknown_error))
		}
	}

	private fun rebuildWgInterface(included: Collection<String>, excluded: Collection<String>, config: Config): Interface {
		return with(config.`interface`) {
			Interface.Builder().apply {
				addAddresses(addresses)
				addDnsSearchDomains(dnsSearchDomains)
				excludeApplications(excluded)
				includeApplications(included)
				setKeyPair(keyPair)
				if (listenPort.isPresent) setListenPort(listenPort.get())
				if (mtu.isPresent) setMtu(mtu.get())
				preUp.forEach { parsePreUp(it) }
				postUp.forEach { parsePostUp(it) }
				preDown.forEach { parsePreDown(it) }
				postDown.forEach { parsePostDown(it) }
			}.build()
		}
	}

	private fun rebuildAmInterface(
		included: Collection<String>,
		excluded: Collection<String>,
		config: org.amnezia.awg.config.Config,
	): org.amnezia.awg.config.Interface {
		return with(config.`interface`) {
			org.amnezia.awg.config.Interface.Builder().apply {
				addAddresses(addresses)
				addDnsSearchDomains(dnsSearchDomains)
				excludeApplications(excluded)
				includeApplications(included)
				setKeyPair(keyPair)
				if (listenPort.isPresent) setListenPort(listenPort.get())
				if (mtu.isPresent) setMtu(mtu.get())
				preUp.forEach { parsePreUp(it) }
				postUp.forEach { parsePostUp(it) }
				preDown.forEach { parsePreDown(it) }
				postDown.forEach { parsePostDown(it) }
				if (junkPacketCount.isPresent) setJunkPacketCount(junkPacketCount.get())
				if (junkPacketMinSize.isPresent) setJunkPacketMinSize(junkPacketMinSize.get())
				if (junkPacketMaxSize.isPresent) setJunkPacketMaxSize(junkPacketMaxSize.get())
				if (initPacketJunkSize.isPresent) setInitPacketJunkSize(initPacketJunkSize.get())
				if (responsePacketJunkSize.isPresent) setResponsePacketJunkSize(responsePacketJunkSize.get())
				if (initPacketMagicHeader.isPresent) setInitPacketMagicHeader(initPacketMagicHeader.get())
				if (responsePacketMagicHeader.isPresent) setResponsePacketMagicHeader(responsePacketMagicHeader.get())
				if (underloadPacketMagicHeader.isPresent) setUnderloadPacketMagicHeader(underloadPacketMagicHeader.get())
				if (transportPacketMagicHeader.isPresent) setTransportPacketMagicHeader(transportPacketMagicHeader.get())
			}.build()
		}
	}
}
