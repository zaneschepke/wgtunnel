package com.zaneschepke.wireguardautotunnel.core.tunnel

import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.WireGuardAutoTunnel
import com.zaneschepke.wireguardautotunnel.core.network.NetworkMonitor
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
import com.zaneschepke.wireguardautotunnel.util.extensions.cancelWithMessage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
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

	private val _activeTunnels = MutableStateFlow<Map<Int, TunnelState>>(emptyMap())

	private val tunnelJobs = mutableMapOf<TunnelConf, Job>()

	private val isNetworkAvailable = AtomicBoolean(false)

	init {
		applicationScope.launch(ioDispatcher) {
			launch {
				startNetworkJob()
			}
			tunnels.collect { tuns ->
				val previousTuns = tunnelJobs.keys.toSet()
				val newTuns = tuns - previousTuns
				val removedItems = previousTuns - tuns.toSet()

				newTuns.forEach { tun ->
					Timber.d("Starting tunnel jobs for tun ${tun.name}")
					tunnelJobs[tun] = startTunnelJobs(tun)
				}

				removedItems.forEach { tun ->
					tunnelJobs[tun]?.cancelWithMessage("Canceling tunnel jobs for tunnel: ${tun.name}")
					tunnelJobs.remove(tun)
					_activeTunnels.update { it - tun.id }
					serviceManager.updateTunnelTile()
				}
			}
		}
	}

	private fun startTunnelJobs(tunnel: TunnelConf) = applicationScope.launch(ioDispatcher) {
		launch {
			startTunnelStatisticsJob(tunnel)
		}
		launch {
			startPingJob(tunnel)
		}
		launch {
			startTunnelConfigChangeJob(tunnel)
		}
		launch {
			startStateJob(tunnel)
		}
	}

	override suspend fun bounceTunnel(tunnelConf: TunnelConf) {
		if (tunnels.value.any { it.id == tunnelConf.id }) {
			toggleTunnel(tunnelConf, TunnelStatus.DOWN)
			toggleTunnel(tunnelConf, TunnelStatus.UP)
		}
	}

	override suspend fun setBackendState(backendState: BackendState, allowedIps: Collection<String>) {
	}

	override suspend fun runningTunnelNames(): Set<String> {
		return emptySet()
	}

	override val activeTunnels: StateFlow<Map<Int, TunnelState>>
		get() = _activeTunnels.asStateFlow()

	override suspend fun startTunnel(tunnelConf: TunnelConf) {
		serviceManager.startBackgroundService(tunnelConf)
		appDataRepository.tunnels.save(tunnelConf.copy(isActive = true))
		addToActiveTunnels(tunnelConf)
	}

	override suspend fun stopTunnel(tunnelConf: TunnelConf?) {
	}

	open suspend fun toggleTunnel(tunnelConf: TunnelConf, state: TunnelStatus) {
	}

	open suspend fun getStatistics(tunnelConf: TunnelConf): TunnelStatistics {
		throw NotImplementedError("Get statistics not implemented in base class")
	}

	internal suspend fun onTunnelStop(tunnelConf: TunnelConf) {
		appDataRepository.tunnels.save(tunnelConf.copy(isActive = false))
		removeFromActiveTunnels(tunnelConf)
		if (tunnels.value.isEmpty()) serviceManager.stopBackgroundService()
	}

	internal suspend fun stopAllTunnels() {
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
		networkMonitor.status.distinctUntilChanged().collect {
			isNetworkAvailable.set(!it.allOffline)
		}
	}

	private suspend fun startStateJob(tunnel: TunnelConf) {
		tunnel.state.collect { state ->
			_activeTunnels.update {
				it + (tunnel.id to state)
			}
			serviceManager.updateTunnelTile()
		}
	}

	private suspend fun startPingJob(tunnel: TunnelConf) = coroutineScope {
		while (isActive) {
			if (isNetworkAvailable.get() && tunnel.isActive) {
				val pingResult = tunnel.pingTunnel(ioDispatcher)
				handlePingResult(tunnel, pingResult)
			}
			delay(tunnel.pingInterval ?: Constants.PING_INTERVAL)
		}
	}

	private suspend fun handlePingResult(tunnel: TunnelConf, pingResult: List<Boolean>) {
		if (pingResult.contains(false)) {
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

	private suspend fun startTunnelConfigChangeJob(tunnel: TunnelConf) = coroutineScope {
		appDataRepository.tunnels.flow.collect { storageTuns ->
			storageTuns.firstOrNull { it.id == tunnel.id }?.let { storageTun ->
				if (!tunnel.isQuickConfigMatching(storageTun) || !tunnel.isPingConfigMatching(storageTun)) {
					bounceTunnel(tunnel)
				}
			}
		}
	}

	private suspend fun startTunnelStatisticsJob(tunnel: TunnelConf) = coroutineScope {
		while (isActive) {
			val stats = getStatistics(tunnel)
			tunnel.state.update {
				it.copy(statistics = stats)
			}
			delay(CHECK_INTERVAL)
		}
	}
}
