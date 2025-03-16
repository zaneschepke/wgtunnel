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
import com.zaneschepke.wireguardautotunnel.util.extensions.toBackendError
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
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

	internal val tunnels = MutableStateFlow<List<TunnelConf>>(emptyList())
	private val _activeTunnels = MutableStateFlow<Map<Int, TunnelState>>(emptyMap())
	override val activeTunnels = _activeTunnels.asStateFlow()

	protected val tunnelJobs = ConcurrentHashMap<Int, MutableList<Job>>()
	private val mutex = Mutex()
	private val isNetworkConnected = MutableStateFlow(true)

	init {
		applicationScope.launch(ioDispatcher) {
			launch { monitorNetworkStatus() }
			launch { monitorTunnelConfigChanges() }
			launch { monitorTunnels() }
		}
	}

	private suspend fun monitorTunnels() {
		tunnels.collectLatest { newTunnels ->
			mutex.withLock {
				val previousIds = tunnelJobs.keys
				val currentIds = newTunnels.map { it.id }.toSet()
				val added = newTunnels.filter { it.id !in previousIds && it.isActive }
				val removed = previousIds - currentIds

				added.forEach { tunnel ->
					Timber.d("Starting jobs for tunnel ${tunnel.id}: ${tunnel.tunName}")
					if (tunnelJobs[tunnel.id] == null) {
						tunnelJobs[tunnel.id] = mutableListOf(startTunnelJobs(tunnel))
					}
				}

				removed.forEach { id ->
					Timber.d("Stopping jobs for tunnel $id")
					tunnelJobs[id]?.forEach { it.cancelAndJoin() }
					tunnelJobs.remove(id)
					_activeTunnels.update { it - id }
					serviceManager.updateTunnelTile()
				}
			}
		}
	}

	protected fun startTunnelJobs(tunnel: TunnelConf): Job {
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
				updateTunnelState(tunnel.id, stats = stats)
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
		val backendError = when(throwable) {
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

	protected fun updateTunnelState(tunnelId: Int, state: TunnelStatus? = null, stats: TunnelStatistics? = null) {
		applicationScope.launch(ioDispatcher) {
			mutex.withLock {
				_activeTunnels.update { current ->
					val existing = current[tunnelId] ?: TunnelState()
					val newState = state ?: existing.state
					if (existing.state == newState && stats == null) {
						Timber.d("Skipping redundant state update for $tunnelId: $newState")
						current
					} else {
						val updated = existing.copy(
							state = newState,
							statistics = stats ?: existing.statistics
						)
						current + (tunnelId to updated)
					}
				}
			}
		}
	}

	protected suspend fun configureTunnel(tunnelConf: TunnelConf) {
		tunnelConf.setStateChangeCallback { state ->
			Timber.d("State change callback triggered for tunnel ${tunnelConf.id}: ${tunnelConf.tunName} with state $state at ${System.currentTimeMillis()}")
			when (state) {
				is Tunnel.State -> updateTunnelState(tunnelConf.id, state.asTunnelState())
				is org.amnezia.awg.backend.Tunnel.State -> updateTunnelState(tunnelConf.id, state.asTunnelState())
			}
			applicationScope.launch(ioDispatcher) { serviceManager.updateTunnelTile() }
		}
	}

	override fun startTunnel(tunnelConf: TunnelConf) {
		applicationScope.launch(ioDispatcher) {
			mutex.withLock {
				val currentState = _activeTunnels.value[tunnelConf.id]?.state
				if (currentState == TunnelStatus.UP || currentState == TunnelStatus.STARTING) {
					Timber.w("Tunnel ${tunnelConf.id} is already $currentState, skipping start (possible duplicate call)")
					return@launch
				}

				val existingNames = tunnels.value.map { it.tunName }.toSet()
				if (tunnelConf.tunName in existingNames && tunnels.value.any { it.id != tunnelConf.id && it.tunName == tunnelConf.tunName }) {
					Timber.w("Duplicate tunName ${tunnelConf.tunName} detected for tunnel ${tunnelConf.id}")
				}

				val lockedConf = tunnelConf.copyWithCallback(isActive = true)
				Timber.d("Starting tunnel with TunnelConf: $lockedConf")
				if (tunnels.value.isEmpty()) {
					Timber.d("No active tunnels, starting background service for ${lockedConf.id}")
					serviceManager.startTunnelForegroundService(lockedConf)
				} else {
					Timber.d("Tunnels already active, updating service notification for ${lockedConf.id}")
					serviceManager.updateTunnelForegroundServiceNotification(lockedConf)
				}
				appDataRepository.tunnels.save(lockedConf)
				tunnels.update { current ->
					current.filter { it.id != lockedConf.id } + lockedConf
				}
			}
		}
	}

	override fun stopTunnel(tunnelConf: TunnelConf?) {
		tunnelConf?.let {
			applicationScope.launch(ioDispatcher) {
				mutex.withLock {
					val lockedConf = it.copyWithCallback(isActive = false)
					Timber.d("Stopping tunnel with TunnelConf: $lockedConf")
					tunnels.update { tunnels -> tunnels.filter { t -> t.id != lockedConf.id } }
					appDataRepository.tunnels.save(lockedConf)
					if (tunnels.value.isEmpty()) {
						Timber.d("No tunnels active, stopping background service")
						serviceManager.stopTunnelForegroundService()
					} else {
						Timber.d("Other tunnels still active, updating service notification")
						val nextActive = tunnels.value.firstOrNull { it.isActive }
						if (nextActive != null) {
							Timber.d("Next active tunnel: ${nextActive.id}")
							serviceManager.updateTunnelForegroundServiceNotification(nextActive)
						} else {
							Timber.w("No active tunnels found in _tunnels, forcing service stop")
							serviceManager.stopTunnelForegroundService()
						}
					}
				}
			}
		}
	}

	override suspend fun bounceTunnel(tunnelConf: TunnelConf) {
		stopTunnel(tunnelConf)
		delay(1000)
		startTunnel(tunnelConf)
	}

	private suspend fun monitorNetworkStatus() {
		networkMonitor.getNetworkStatusFlow(includeWifiSsid = false, useRootShell = false)
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
					val current = tunnels.value.find { it.id == stored.id }
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

	override suspend fun runningTunnelNames(): Set<String> = tunnels.value.map { it.tunName }.toSet()
}
