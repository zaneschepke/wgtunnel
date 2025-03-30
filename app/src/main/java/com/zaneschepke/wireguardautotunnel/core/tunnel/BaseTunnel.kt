package com.zaneschepke.wireguardautotunnel.core.tunnel

import com.wireguard.android.backend.BackendException
import com.wireguard.android.backend.Tunnel
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
import com.zaneschepke.wireguardautotunnel.util.StringValue
import com.zaneschepke.wireguardautotunnel.util.extensions.asTunnelState
import com.zaneschepke.wireguardautotunnel.util.extensions.toBackendError
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

abstract class BaseTunnel(
	@IoDispatcher private val ioDispatcher: CoroutineDispatcher,
	@ApplicationScope private val applicationScope: CoroutineScope,
	private val appDataRepository: AppDataRepository,
	private val serviceManager: ServiceManager,
	private val notificationManager: NotificationManager,
) : TunnelProvider {

	protected val activeTuns = MutableStateFlow<Map<TunnelConf, TunnelState>>(emptyMap())
	override val activeTunnels = activeTuns.asStateFlow()

	private val isBounce = AtomicBoolean(false)

	protected val mutex = Mutex()

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

	private fun updateTunnelState(tunnelConf: TunnelConf, state: TunnelStatus? = null, stats: TunnelStatistics? = null) {
		applicationScope.launch(ioDispatcher) {
			mutex.withLock {
				activeTuns.update { current ->
					val originalConf = current.getKeyById(tunnelConf.id) ?: tunnelConf
					val existingState = current.getValueById(tunnelConf.id) ?: TunnelState()
					val newState = state ?: existingState.state
					if (newState == TunnelStatus.DOWN) {
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

	protected suspend fun stopActiveTunnels() {
		activeTunnels.value.forEach { (config, state) ->
			if (state.state.isUp()) {
				stopTunnel(config)
				delay(300)
			}
		}
	}

	protected fun configureTunnel(tunnelConf: TunnelConf) {
		Timber.d("Configuring TunnelConf instance: ${tunnelConf.hashCode()}")

		tunnelConf.setStateChangeCallback { state ->
			Timber.d("State change callback triggered for tunnel ${tunnelConf.id}: ${tunnelConf.tunName} with state $state at ${System.currentTimeMillis()}")
			when (state) {
				is Tunnel.State -> updateTunnelState(tunnelConf, state.asTunnelState())
				is org.amnezia.awg.backend.Tunnel.State -> updateTunnelState(tunnelConf, state.asTunnelState())
			}
			serviceManager.updateTunnelTile()
		}
		tunnelConf.setTunnelStatsCallback {
			val stats = getStatistics(tunnelConf)
			updateTunnelState(tunnelConf, null, stats)
		}
		tunnelConf.setBounceTunnelCallback {
			bounceTunnel(tunnelConf)
		}
	}

	override fun startTunnel(tunnelConf: TunnelConf) {
		applicationScope.launch {
			val tunnelCopy = tunnelConf.copyWithCallback(isActive = true)
			if (!isBounce.get()) serviceManager.startTunnelForegroundService(tunnelCopy)
			appDataRepository.tunnels.save(tunnelCopy)
		}
	}

	override fun stopTunnel(tunnelConf: TunnelConf?) {
		if (tunnelConf == null) return
		applicationScope.launch(ioDispatcher) {
			mutex.withLock {
				removeActiveTunnel(tunnelConf)
				val lockedConf = tunnelConf.copyWithCallback(isActive = false)
				appDataRepository.tunnels.save(lockedConf)
				if (activeTuns.value.isEmpty() && !isBounce.get()) return@launch serviceManager.stopTunnelForegroundService()
				val nextActive = activeTuns.value.keys.firstOrNull()
				if (nextActive != null) {
					Timber.d("Next active tunnel: ${nextActive.id}")
					serviceManager.updateTunnelForegroundServiceNotification(nextActive)
				}
			}
		}
	}

	private fun removeActiveTunnel(tunnelConf: TunnelConf) {
		activeTuns.update { current ->
			current.toMutableMap().apply { remove(tunnelConf) }
		}
	}

	override fun bounceTunnel(tunnelConf: TunnelConf) {
		applicationScope.launch(ioDispatcher) {
			isBounce.set(true)
			stopTunnel(tunnelConf)
			delay(300)
			startTunnel(tunnelConf)
			isBounce.set(false)
		}
	}

	override suspend fun runningTunnelNames(): Set<String> = activeTuns.value.keys.map { it.tunName }.toSet()
}
