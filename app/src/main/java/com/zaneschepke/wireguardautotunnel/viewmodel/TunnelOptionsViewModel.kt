package com.zaneschepke.wireguardautotunnel.viewmodel

import androidx.lifecycle.viewModelScope
import com.zaneschepke.wireguardautotunnel.domain.entity.TunnelConf
import com.zaneschepke.wireguardautotunnel.domain.repository.AppDataRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TunnelOptionsViewModel
@Inject
constructor(
	appDataRepository: AppDataRepository,
) : BaseViewModel(appDataRepository) {

	fun onTogglePrimaryTunnel(tunnelConf: TunnelConf) = viewModelScope.launch {
		appDataRepository.tunnels.updatePrimaryTunnel(
			when (tunnelConf.isPrimaryTunnel) {
				true -> null
				false -> tunnelConf
			},
		)
	}

	fun onToggleIpv4(tunnelConf: TunnelConf) = saveTunnel(
		tunnelConf.copy(
			isIpv4Preferred = !tunnelConf.isIpv4Preferred,
		),
	)

	fun onPingIntervalChange(tunnelConf: TunnelConf, interval: String) = saveTunnel(
		tunnelConf.copy(pingInterval = if (interval.isBlank()) null else interval.toLong() * 1000),
	)

	fun onPingCoolDownChange(tunnelConf: TunnelConf, cooldown: String) = saveTunnel(
		tunnelConf.copy(pingCooldown = if (cooldown.isBlank()) null else cooldown.toLong() * 1000),
	)
}
