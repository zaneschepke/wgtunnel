package com.zaneschepke.wireguardautotunnel.core.tunnel

import com.wireguard.android.backend.BackendException
import com.wireguard.android.backend.Tunnel
import com.zaneschepke.networkmonitor.NetworkMonitor
import com.zaneschepke.networkmonitor.NetworkStatus
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.WireGuardAutoTunnel
import com.zaneschepke.wireguardautotunnel.core.notification.NotificationManager
import com.zaneschepke.wireguardautotunnel.core.notification.WireGuardNotification
import com.zaneschepke.wireguardautotunnel.core.service.ServiceManager
import com.zaneschepke.wireguardautotunnel.di.ApplicationScope
import com.zaneschepke.wireguardautotunnel.di.IoDispatcher
import com.zaneschepke.wireguardautotunnel.domain.entity.TunnelConf
import com.zaneschepke.wireguardautotunnel.domain.enums.BackendError
import com.zaneschepke.wireguardautotunnel.domain.enums.TunnelStatus
import com.zaneschepke.wireguardautotunnel.domain.repository.AppDataRepository
import com.zaneschepke.wireguardautotunnel.domain.state.TunnelState
import com.zaneschepke.wireguardautotunnel.domain.state.TunnelStatistics
import com.zaneschepke.wireguardautotunnel.ui.common.snackbar.SnackbarController
import com.zaneschepke.wireguardautotunnel.util.Constants
import com.zaneschepke.wireguardautotunnel.util.StringValue
import com.zaneschepke.wireguardautotunnel.util.extensions.asTunnelState
import com.zaneschepke.wireguardautotunnel.util.extensions.cancelWithMessage
import com.zaneschepke.wireguardautotunnel.util.extensions.toBackendError
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap

abstract class BaseTunnel(
	@IoDispatcher private val ioDispatcher: CoroutineDispatcher,
	@ApplicationScope private val applicationScope: CoroutineScope,
	private val networkMonitor: NetworkMonitor,
	private val appDataRepository: AppDataRepository,
	private val serviceManager: ServiceManager,
	private val notificationManager: NotificationManager,
) : TunnelProvider {

	companion object {
		const val CHECK_INTERVAL = 1000L
	}

	protected val activeTuns = MutableStateFlow<Map<TunnelConf, TunnelState>>(emptyMap())
	override val activeTunnels = activeTuns.asStateFlow()

	private val tunnelJobs = ConcurrentHashMap<Int, MutableList<Job>>()

	protected val mutex = Mutex()
	private val isNetworkConnected = MutableStateFlow(true)

	init {
		applicationScope.launch(ioDispatcher) {
			launch { monitorNetworkStatus() }
			launch { monitorTunnelConfigChanges() }
		}
	}

	private fun startTunnelJobs(tunnel: TunnelConf): Job {
		return applicationScope.launch(ioDispatcher) {
			val jobs = mutableListOf<Job>()
			jobs += launch { updateTunnelStatistics(tunnel) }
			if (tunnel.isPingEnabled) jobs += launch { monitorTunnelPing(tunnel) }
			jobs.forEach { it.join() }
		}
	}

	private suspend fun updateTunnelStatistics(tunnel: TunnelConf) {
		while (true) {
			runCatching {
				val stats = getStatistics(tunnel)
				updateTunnelState(tunnel, stats = stats)
			}.onFailure { e ->
				Timber.e(e, "Failed to update stats for ${tunnel.tunName}")
			}
			delay(CHECK_INTERVAL)
		}
	}

	private suspend fun monitorTunnelPing(tunnel: TunnelConf) {
		while (true) {
			runCatching {
				if (isNetworkConnected.value && tunnel.isActive) {
					val pingSuccess = tunnel.isTunnelPingable(ioDispatcher)
					if (!pingSuccess) bounceTunnel(tunnel)
				}
			}.onFailure { e ->
				Timber.e(e, "Ping failed for ${tunnel.tunName}")
			}
			delay(tunnel.pingInterval ?: Constants.PING_INTERVAL)
		}
	}

	protected fun handleBackendThrowable(throwable: Throwable) {
		val backendError = when (throwable) {
			is BackendException -> throwable.toBackendError()
			is org.amnezia.awg.backend.BackendException -> throwable.toBackendError()
			else -> BackendError.Unknown
		}
		val message = when (backendError) {
			BackendError.Config -> StringValue.StringResource(R.string.start_failed_config)
			BackendError.DNS -> StringValue.StringResource(R.string.dns_error)
			BackendError.Unauthorized -> StringValue.StringResource(R.string.unauthorized)
			BackendError.Unknown -> StringValue.StringResource(R.string.unknown_error)
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

	protected fun updateTunnelState(tunnelConf: TunnelConf, state: TunnelStatus? = null, stats: TunnelStatistics? = null) {
		applicationScope.launch(ioDispatcher) {
			mutex.withLock {
				activeTuns.update { current ->
					val originalConf = current.getKeyById(tunnelConf.id) ?: tunnelConf
					val existingState = current.getValueById(tunnelConf.id) ?: TunnelState()
					val newState = state ?: existingState.state
					if (newState == TunnelStatus.DOWN) {
						// Remove tunnel from activeTunnels when it goes DOWN
						Timber.d("Removing tunnel ${tunnelConf.id} from activeTunnels as state is DOWN")
						current - originalConf
					} else if (existingState.state == newState && stats == null) {
						Timber.d("Skipping redundant state update for ${tunnelConf.id}: $newState")
						current
					} else {
						val updated = existingState.copy(
							state = newState,
							statistics = stats ?: existingState.statistics,
						)
						current + (originalConf to updated)
					}
				}
			}
		}
	}

	protected suspend fun configureTunnel(tunnelConf: TunnelConf) {
		// setup state change callback
		tunnelConf.setStateChangeCallback { state ->
			Timber.d("State change callback triggered for tunnel ${tunnelConf.id}: ${tunnelConf.tunName} with state $state at ${System.currentTimeMillis()}")
			when (state) {
				is Tunnel.State -> updateTunnelState(tunnelConf, state.asTunnelState())
				is org.amnezia.awg.backend.Tunnel.State -> updateTunnelState(tunnelConf, state.asTunnelState())
			}
			applicationScope.launch(ioDispatcher) { serviceManager.updateTunnelTile() }
		}

		activeTuns.update { current ->
			current.filter { it.key != tunnelConf } + (tunnelConf to TunnelState())
		}
	}

	protected suspend fun onStartSuccess(tunnelConf: TunnelConf) {
		val tunnelCopy = tunnelConf.copyWithCallback(isActive = true)

		// start service
		if (activeTuns.value.isEmpty()) {
			serviceManager.startTunnelForegroundService(tunnelCopy)
		} else {
			serviceManager.updateTunnelForegroundServiceNotification(tunnelCopy)
		}
		// save active
		appDataRepository.tunnels.save(tunnelCopy)
		// start tunnel jobs
		tunnelJobs[tunnelCopy.id] = mutableListOf(startTunnelJobs(tunnelConf))
	}

	override fun startTunnel(tunnelConf: TunnelConf) {
		throw NotImplementedError("Must be implemented by subclass")
	}

	override fun stopTunnel(tunnelConf: TunnelConf?) {
		tunnelConf?.let {
			applicationScope.launch(ioDispatcher) {
				mutex.withLock {
					removeActiveTunnel(tunnelConf)
					tunnelJobs[tunnelConf.id]?.forEach { it.cancelWithMessage("Cancel tunnel job") }
					tunnelJobs.remove(tunnelConf.id)
					val lockedConf = it.copyWithCallback(isActive = false)
					appDataRepository.tunnels.save(lockedConf)

					// TODO improve to handle multiple tunnels
					if (activeTuns.value.isEmpty()) {
						Timber.d("No tunnels active, stopping background service")
						serviceManager.stopTunnelForegroundService()
					} else {
						Timber.d("Other tunnels still active, updating service notification")
						val nextActive = activeTuns.value.keys.firstOrNull()
						if (nextActive != null) {
							Timber.d("Next active tunnel: ${nextActive.id}")
							serviceManager.updateTunnelForegroundServiceNotification(nextActive)
						}
					}
				}
			}
		}
	}

	private fun removeActiveTunnel(tunnelConf: TunnelConf) {
		activeTuns.update { current ->
			current.toMutableMap().apply { remove(tunnelConf) }
		}
	}

	override suspend fun bounceTunnel(tunnelConf: TunnelConf) {
		stopTunnel(tunnelConf)
		delay(1000)
		startTunnel(tunnelConf)
	}

	private suspend fun monitorNetworkStatus() {
		networkMonitor.networkStatusFlow
			.flowOn(ioDispatcher)
			.collectLatest { status ->
				val isAvailable = status !is NetworkStatus.Disconnected
				isNetworkConnected.value = isAvailable
				Timber.d("Network status: $isAvailable")
			}
	}

	private suspend fun monitorTunnelConfigChanges() {
		appDataRepository.tunnels.flow.collectLatest { storedTunnels ->
			mutex.withLock {
				storedTunnels.forEach { stored ->
					val current = activeTuns.value.keys.find { it.id == stored.id }
					if (current != null && !current.isQuickConfigMatching(stored)) {
						Timber.d("Config changed for ${stored.id}, bouncing")
						bounceTunnel(stored)
					}
				}
			}
		}
	}

	override fun getStatistics(tunnelConf: TunnelConf): TunnelStatistics? {
		throw NotImplementedError("Must be implemented by subclass")
	}

	override suspend fun runningTunnelNames(): Set<String> = activeTuns.value.keys.map { it.tunName }.toSet()
}
