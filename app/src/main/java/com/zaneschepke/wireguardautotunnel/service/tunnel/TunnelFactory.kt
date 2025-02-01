package com.zaneschepke.wireguardautotunnel.service.tunnel

import android.content.Context
import androidx.compose.runtime.MutableState
import com.wireguard.android.backend.Backend
import com.wireguard.android.backend.RootTunnelActionHandler
import com.wireguard.android.backend.WgQuickBackend
import com.wireguard.android.util.RootShell
import com.wireguard.android.util.ToolsInstaller
import com.zaneschepke.wireguardautotunnel.data.domain.TunnelConfig
import com.zaneschepke.wireguardautotunnel.data.repository.AppDataRepository
import com.zaneschepke.wireguardautotunnel.service.foreground.ServiceManager
import com.zaneschepke.wireguardautotunnel.service.network.NetworkService
import com.zaneschepke.wireguardautotunnel.service.tunnel.model.VpnState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

class TunnelFactory @Inject constructor(
	private val ioDispatcher: CoroutineDispatcher,
	private val applicationScope: CoroutineScope,
	private val serviceManager: ServiceManager,
	private val appDataRepository: AppDataRepository,
	private val amBackend: org.amnezia.awg.backend.Backend,
	private val internetConnectivityService: NetworkService,
	private val context: Context,
) : TunnelsManager {

	private val _tunnelStates = MutableStateFlow(mapOf<Int, VpnState>())
	override val tunnelStates: Flow<Map<Int, VpnState>>
		get() = _tunnelStates.asStateFlow()

	private val _tunnels = MutableStateFlow(mapOf<Int, TunnelService>())

	private suspend fun createTunnel(tunnelConfig: TunnelConfig, isKernelTunnel: Boolean): TunnelService {
		if(!isKernelTunnel){
			_tunnels.value.forEach {
				it.value.stopTunnel()
			}
			_tunnelStates.update {
				it.toMutableMap().apply {
					this.clear()
				}
			}
			_tunnels.update {
				it.toMutableMap().apply {
					this.clear()
				}
			}
			return UserspaceTunnel(
				ioDispatcher,
				applicationScope,
				serviceManager,
				appDataRepository,
				amBackend,
				internetConnectivityService,
				tunnelConfig,
				::onVpnStateChange,
				::onStop
			)
		}

		val rootShell = RootShell(context)
		val tunnel = KernelTunnel(
			ioDispatcher,
			applicationScope,
			serviceManager,
			appDataRepository,
			WgQuickBackend(context, rootShell, ToolsInstaller(context, rootShell), RootTunnelActionHandler(rootShell)),
			internetConnectivityService,
			tunnelConfig,
			::onVpnStateChange,
			::onStop
		)
		return tunnel
	}

	override suspend fun getTunnel(tunnelConfig: TunnelConfig, isKernelTunnel: Boolean): TunnelService {
		return _tunnels.value[tunnelConfig.id] ?: createTunnel(tunnelConfig, isKernelTunnel).also { service -> _tunnels.update {
			it.toMutableMap().apply {
				this[tunnelConfig.id] = service
			}
		} }
	}

	fun onVpnStateChange(tunnelConfig : TunnelConfig, state: VpnState) {
		_tunnelStates.update {
			it.toMutableMap().apply {
				this[tunnelConfig.id] = state
			}
		}
	}

	fun onStop(tunnelConfig: TunnelConfig) {
		_tunnels.update {
			it.toMutableMap().apply {
				remove(tunnelConfig.id)
			}
		}
		_tunnelStates.update {
			it.toMutableMap().apply {
				remove(tunnelConfig.id)
			}
		}
	}
}
