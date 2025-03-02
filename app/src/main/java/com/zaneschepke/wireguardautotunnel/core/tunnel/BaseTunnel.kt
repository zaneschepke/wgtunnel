package com.zaneschepke.wireguardautotunnel.core.tunnel

import com.wireguard.android.backend.Tunnel
import com.zaneschepke.networkmonitor.NetworkMonitor
import com.zaneschepke.networkmonitor.NetworkStatus
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.WireGuardAutoTunnel
import com.zaneschepke.wireguardautotunnel.core.notification.NotificationManager
import com.zaneschepke.wireguardautotunnel.core.notification.WireGuardNotification
import com.zaneschepke.wireguardautotunnel.core.service.ServiceManager
import com.zaneschepke.wireguardautotunnel.core.tunnel.TunnelProvider.Companion.CHECK_INTERVAL
import com.zaneschepke.wireguardautotunnel.di.ApplicationScope
import com.zaneschepke.wireguardautotunnel.di.IoDispatcher
import com.zaneschepke.wireguardautotunnel.domain.entity.TunnelConf
import com.zaneschepke.wireguardautotunnel.domain.enums.BackendError
import com.zaneschepke.wireguardautotunnel.domain.enums.BackendState
import com.zaneschepke.wireguardautotunnel.domain.enums.TunnelStatus
import com.zaneschepke.wireguardautotunnel.domain.repository.AppDataRepository
import com.zaneschepke.wireguardautotunnel.domain.state.TunnelState
import com.zaneschepke.wireguardautotunnel.domain.state.TunnelStatistics
import com.zaneschepke.wireguardautotunnel.ui.common.snackbar.SnackbarController
import com.zaneschepke.wireguardautotunnel.util.Constants
import com.zaneschepke.wireguardautotunnel.util.StringValue
import com.zaneschepke.wireguardautotunnel.util.extensions.asTunnelState
import com.zaneschepke.wireguardautotunnel.util.extensions.cancelWithMessage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

open class BaseTunnel(
	@IoDispatcher private val ioDispatcher: CoroutineDispatcher,
	@ApplicationScope private val applicationScope: CoroutineScope,
	private val networkMonitor: NetworkMonitor,
	private val appDataRepository: AppDataRepository,
	private val serviceManager: ServiceManager,
	private val notificationManager: NotificationManager,
) : TunnelProvider {

	internal val tunnels = MutableStateFlow<List<TunnelConf>>(emptyList())

	private val _tunnelStates = MutableStateFlow<Map<Int, TunnelState>>(emptyMap())

	private val tunnelJobs = ConcurrentHashMap<Int, Job>()

	private val isNetworkAvailable = AtomicBoolean(false)

	init {
		applicationScope.launch(ioDispatcher) {
			launch { startNetworkJob() }
			launch { monitorTunnelConfigChanges() }
			tunnels.collect { tuns ->
				val previousTunIds = tunnelJobs.keys.toSet()
				val currentTunIds = tuns.map { it.id }.toSet()
				val newTuns = tuns.filter { it.id !in previousTunIds }
				val removedTunIds = previousTunIds - currentTunIds

				newTuns.forEach { tun ->
					Timber.d("Starting tunnel jobs for tun ${tun.name} (ID: ${tun.id})")
					tunnelJobs[tun.id] = startTunnelJobs(tun)
				}

				removedTunIds.forEach { tunId ->
					tunnelJobs[tunId]?.cancelWithMessage("Canceling tunnel jobs for tunnel ID: $tunId")
					tunnelJobs.remove(tunId)
					_tunnelStates.update { it - tunId }
					serviceManager.updateTunnelTile()
				}
			}
		}
	}

	private fun startTunnelJobs(tunnel: TunnelConf) = applicationScope.launch(ioDispatcher) {
		launch { startTunnelStatisticsJob(tunnel) }
		if (tunnel.isPingEnabled) launch { startPingJob(tunnel) }
	}

	private fun updateTunnelState(tunnelId: Int, newState: TunnelStatus) {
		Timber.d("Updating tunnel state for ID $tunnelId to $newState")
		_tunnelStates.update { current ->
			val currentState = current[tunnelId]
			val updatedState = currentState?.copy(state = newState) ?: TunnelState(state = newState)
			val newMap = current + (tunnelId to updatedState)
			Timber.d("New tunnel states: $newMap")
			newMap
		}
	}

	internal fun beforeStartTunnel(tunnelConf: TunnelConf) {
		tunnelConf.setStateChangeCallback { state ->
			Timber.d("New tunnel state $state")
			when (state) {
				is Tunnel.State -> updateTunnelState(tunnelConf.id, state.asTunnelState())
				is org.amnezia.awg.backend.Tunnel.State -> updateTunnelState(tunnelConf.id, state.asTunnelState())
			}
			applicationScope.launch(ioDispatcher) {
				serviceManager.updateTunnelTile()
			}
		}
	}

	override fun startTunnel(tunnelConf: TunnelConf) {
		applicationScope.launch(ioDispatcher) {
			serviceManager.startBackgroundService(tunnelConf)
			appDataRepository.tunnels.save(tunnelConf.copy(isActive = true))
			addToActiveTunnels(tunnelConf)
		}
	}

	override fun stopTunnel(tunnelConf: TunnelConf?) {
		// Default empty implementation; subclasses override
	}

	override suspend fun bounceTunnel(tunnelConf: TunnelConf) {
		stopTunnel(tunnelConf)
		delay(1000)
		startTunnel(tunnelConf)
	}

	override suspend fun setBackendState(backendState: BackendState, allowedIps: Collection<String>) {
		// Default empty implementation
	}

	override suspend fun runningTunnelNames(): Set<String> {
		return emptySet()
	}

	override fun getStatistics(tunnelConf: TunnelConf): TunnelStatistics {
		throw NotImplementedError("Get statistics not implemented in base class")
	}

	override val activeTunnels: StateFlow<Map<Int, TunnelState>>
		get() = _tunnelStates.asStateFlow()

	internal suspend fun onTunnelStop(tunnelConf: TunnelConf) {
		appDataRepository.tunnels.save(tunnelConf.copy(isActive = false))
		removeFromActiveTunnels(tunnelConf)
		if (tunnels.value.isEmpty()) serviceManager.stopBackgroundService()
	}

	internal fun stopAllTunnels() {
		tunnels.value.forEach {
			stopTunnel(it)
		}
	}

	private fun addToActiveTunnels(conf: TunnelConf) {
		tunnels.update {
			it.toMutableList().apply {
				add(conf)
			}
		}
	}

	private fun removeFromActiveTunnels(conf: TunnelConf) {
		tunnels.update {
			it.toMutableList().apply {
				remove(conf)
			}
		}
	}

	private suspend fun startNetworkJob() = coroutineScope {
		networkMonitor.getNetworkStatusFlow(includeWifiSsid = false, useRootShell = false)
			.flowOn(ioDispatcher).collect {
				isNetworkAvailable.set(it !is NetworkStatus.Disconnected)
			}
	}

	private suspend fun startPingJob(tunnel: TunnelConf) = coroutineScope {
		while (isActive) {
			runCatching {
				if (isNetworkAvailable.get() && tunnel.isActive) {
					val pingSuccess = tunnel.isTunnelPingable(ioDispatcher)
					handlePingResult(tunnel, pingSuccess)
				}
				delay(tunnel.pingInterval ?: Constants.PING_INTERVAL)
			}
		}
	}

	private suspend fun handlePingResult(tunnel: TunnelConf, pingSuccess: Boolean) {
		if (!pingSuccess) {
			if (isNetworkAvailable.get()) {
				Timber.i("Ping result: target was not reachable, bouncing the tunnel")
				bounceTunnel(tunnel)
				delay(tunnel.pingCooldown ?: Constants.PING_COOLDOWN)
			} else {
				Timber.i("Ping result: target was not reachable, but no network available")
			}
		} else {
			Timber.i("Ping result: all ping targets were reached successfully")
		}
	}

	internal fun handleBackendThrowable(backendError: BackendError) {
		val message = when (backendError) {
			BackendError.Config -> StringValue.StringResource(R.string.start_failed_config)
			BackendError.DNS -> StringValue.StringResource(R.string.dns_error)
			BackendError.Unauthorized -> StringValue.StringResource(R.string.unauthorized)
		}
		if (WireGuardAutoTunnel.isForeground()) {
			SnackbarController.showMessage(message)
		} else {
			notificationManager.show(
				NotificationManager.VPN_NOTIFICATION_ID,
				notificationManager.createNotification(
					WireGuardNotification.NotificationChannels.VPN,
					title = StringValue.StringResource(R.string.tunne_start_failed_title),
					description = message,
				),
			)
		}
	}

	private suspend fun monitorTunnelConfigChanges() = coroutineScope {
		appDataRepository.tunnels.flow.collect { storageTuns ->
			storageTuns.forEach { storageTun ->
				val currentTun = tunnels.value.firstOrNull { it.id == storageTun.id }
				if (currentTun != null) {
					if (!currentTun.isQuickConfigMatching(storageTun)) {
						Timber.d("Tunnel config changed for ID $storageTun, bouncing tunnel")
						bounceTunnel(storageTun)
					}
				}
			}
		}
	}

	private suspend fun startTunnelStatisticsJob(tunnel: TunnelConf) = coroutineScope {
		while (this.isActive) {
			runCatching {
				val stats = getStatistics(tunnel)
				_tunnelStates.update { currentStates ->
					val updatedState = currentStates[tunnel.id]?.copy(statistics = stats)
						?: TunnelState(statistics = stats)
					currentStates + (tunnel.id to updatedState)
				}
				delay(CHECK_INTERVAL)
			}.onFailure { exception ->
				Timber.e(exception, "Failed to update tunnel statistics for ${tunnel.tunName}")
			}
		}
	}
}
