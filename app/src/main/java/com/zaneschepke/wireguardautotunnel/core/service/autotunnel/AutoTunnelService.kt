package com.zaneschepke.wireguardautotunnel.core.service.autotunnel

import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.zaneschepke.networkmonitor.NetworkMonitor
import com.zaneschepke.networkmonitor.NetworkStatus
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.core.notification.NotificationManager
import com.zaneschepke.wireguardautotunnel.core.notification.WireGuardNotification
import com.zaneschepke.wireguardautotunnel.core.service.ServiceManager
import com.zaneschepke.wireguardautotunnel.core.tunnel.TunnelManager
import com.zaneschepke.wireguardautotunnel.di.IoDispatcher
import com.zaneschepke.wireguardautotunnel.di.MainImmediateDispatcher
import com.zaneschepke.wireguardautotunnel.domain.entity.AppSettings
import com.zaneschepke.wireguardautotunnel.domain.entity.TunnelConf
import com.zaneschepke.wireguardautotunnel.domain.enums.BackendState
import com.zaneschepke.wireguardautotunnel.domain.enums.NotificationAction
import com.zaneschepke.wireguardautotunnel.domain.events.AutoTunnelEvent
import com.zaneschepke.wireguardautotunnel.domain.events.KillSwitchEvent
import com.zaneschepke.wireguardautotunnel.domain.repository.AppDataRepository
import com.zaneschepke.wireguardautotunnel.domain.state.AutoTunnelState
import com.zaneschepke.wireguardautotunnel.domain.state.NetworkState
import com.zaneschepke.wireguardautotunnel.util.Constants
import com.zaneschepke.wireguardautotunnel.util.extensions.Tunnels
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Provider

@AndroidEntryPoint
class AutoTunnelService : LifecycleService() {

	@Inject
	lateinit var networkMonitor: NetworkMonitor

	@Inject
	lateinit var appDataRepository: Provider<AppDataRepository>

	@Inject
	lateinit var notificationManager: NotificationManager

	@Inject
	@IoDispatcher
	lateinit var ioDispatcher: CoroutineDispatcher

	@Inject
	lateinit var serviceManager: ServiceManager

	@Inject
	@MainImmediateDispatcher
	lateinit var mainImmediateDispatcher: CoroutineDispatcher

	@Inject
	lateinit var tunnelManager: TunnelManager

	private val defaultState = AutoTunnelState()

	private val autoTunnelStateFlow = MutableStateFlow(defaultState)

	private var wakeLock: PowerManager.WakeLock? = null

	private var killSwitchJob: Job? = null

	override fun onCreate() {
		super.onCreate()
		serviceManager.autoTunnelService.complete(this)
		lifecycleScope.launch(mainImmediateDispatcher) {
			runCatching {
				launchWatcherNotification()
			}.onFailure {
				Timber.e(it)
			}
		}
	}

	override fun onBind(intent: Intent): IBinder? {
		super.onBind(intent)
		return null
	}

	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
		super.onStartCommand(intent, flags, startId)
		Timber.d("onStartCommand executed with startId: $startId")
		serviceManager.autoTunnelService.complete(this)
		start()
		return START_STICKY
	}

	fun start() {
		kotlin.runCatching {
			lifecycleScope.launch(mainImmediateDispatcher) {
				launchWatcherNotification()
				initWakeLock()
			}
			startAutoTunnelJob()
			startAutoTunnelStateJob()
			killSwitchJob = startKillSwitchJob()
		}.onFailure {
			Timber.e(it)
		}
	}

	fun stop() {
		wakeLock?.let { if (it.isHeld) it.release() }
		stopSelf()
	}

	override fun onDestroy() {
		serviceManager.autoTunnelService = CompletableDeferred()
		restoreVpnKillSwitch()
		super.onDestroy()
	}

	private fun restoreVpnKillSwitch() {
		with(autoTunnelStateFlow.value) {
			if (settings.isVpnKillSwitchEnabled && tunnelManager.getBackendState() != BackendState.KILL_SWITCH_ACTIVE) {
				killSwitchJob?.cancel()
				val allowedIps = if (settings.isLanOnKillSwitchEnabled) TunnelConf.LAN_BYPASS_ALLOWED_IPS else emptyList()
				tunnelManager.setBackendState(BackendState.KILL_SWITCH_ACTIVE, allowedIps)
			}
		}
	}

	private fun launchWatcherNotification(description: String = getString(R.string.monitoring_state_changes)) {
		val notification =
			notificationManager.createNotification(
				WireGuardNotification.NotificationChannels.AUTO_TUNNEL,
				title = getString(R.string.auto_tunnel_title),
				description = description,
				actions = listOf(
					notificationManager.createNotificationAction(NotificationAction.AUTO_TUNNEL_OFF),
				),
			)
		ServiceCompat.startForeground(
			this,
			NotificationManager.AUTO_TUNNEL_NOTIFICATION_ID,
			notification,
			Constants.SYSTEM_EXEMPT_SERVICE_TYPE_ID,
		)
	}

	private fun initWakeLock() {
		wakeLock = (getSystemService(POWER_SERVICE) as PowerManager).run {
			val tag = this.javaClass.name
			newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "$tag::lock").apply {
				try {
					Timber.i("Initiating wakelock with 10 min timeout")
					acquire(Constants.BATTERY_SAVER_WATCHER_WAKE_LOCK_TIMEOUT)
				} finally {
					release()
				}
			}
		}
	}

	private fun buildNetworkState(networkStatus: NetworkStatus): NetworkState {
		return with(autoTunnelStateFlow.value.networkState) {
			val wifiName = when (networkStatus) {
				is NetworkStatus.Connected -> {
					networkStatus.wifiSsid
				}
				else -> null
			}
			copy(
				isWifiConnected = networkStatus.wifiConnected,
				isMobileDataConnected = networkStatus.cellularConnected,
				isEthernetConnected = networkStatus.ethernetConnected,
				wifiName = wifiName,
			)
		}
	}

	@OptIn(ExperimentalCoroutinesApi::class)
	private fun startAutoTunnelStateJob() = lifecycleScope.launch(ioDispatcher) {
		combine(
			combineSettings(),
			appDataRepository.get().settings.flow
				.distinctUntilChanged { old, new -> old.isKernelEnabled == new.isKernelEnabled } // Only emit when isKernelEnabled changes
				.flatMapLatest {
					networkMonitor.networkStatusFlow
						.flowOn(ioDispatcher)
						.map { buildNetworkState(it) }
				}
				.distinctUntilChanged(),
		) { double, networkState ->
			AutoTunnelState(
				tunnelManager.activeTunnels.value,
				networkState,
				double.first,
				double.second,
			)
		}.collect { state ->
			autoTunnelStateFlow.update {
				it.copy(
					activeTunnels = state.activeTunnels,
					networkState = state.networkState,
					settings = state.settings,
					tunnels = state.tunnels,
				)
			}
		}
	}

	private fun combineSettings(): Flow<Pair<AppSettings, Tunnels>> {
		return combine(
			appDataRepository.get().settings.flow,
			appDataRepository.get().tunnels.flow.map { tunnels ->
				// isActive is ignored for equality checks so user can manually toggle off tunnel with auto-tunnel
				tunnels.map { it.copy(isActive = false) }
			},
		) { settings, tunnels ->
			Pair(settings, tunnels)
		}.distinctUntilChanged()
	}

	private fun startKillSwitchJob() = lifecycleScope.launch(ioDispatcher) {
		autoTunnelStateFlow.collect {
			if (it == defaultState) return@collect
			when (val event = it.asKillSwitchEvent()) {
				KillSwitchEvent.DoNothing -> Unit
				is KillSwitchEvent.Start -> {
					Timber.d("Starting kill switch")
					tunnelManager.setBackendState(BackendState.KILL_SWITCH_ACTIVE, event.allowedIps)
				}
				KillSwitchEvent.Stop -> {
					Timber.d("Stopping kill switch")
					tunnelManager.setBackendState(BackendState.SERVICE_ACTIVE, emptySet())
				}
			}
		}
	}

	@OptIn(FlowPreview::class)
	private fun startAutoTunnelJob() = lifecycleScope.launch(ioDispatcher) {
		Timber.i("Starting auto-tunnel network event watcher")
		val settings = appDataRepository.get().settings.get()
		Timber.d("Starting with debounce delay of: ${settings.debounceDelaySeconds} seconds")
		autoTunnelStateFlow.debounce(settings.debounceDelayMillis()).collect { watcherState ->
			if (watcherState == defaultState) return@collect
			Timber.d("New auto tunnel state emitted ${watcherState.networkState}")
			when (val event = watcherState.asAutoTunnelEvent()) {
				is AutoTunnelEvent.Start -> (event.tunnelConf ?: appDataRepository.get().getPrimaryOrFirstTunnel())?.let {
					tunnelManager.startTunnel(it)
				}
				// TODO improve this to target specific tunnels to better support multi-tunnel
				is AutoTunnelEvent.Stop -> tunnelManager.stopTunnel()
				AutoTunnelEvent.DoNothing -> Timber.i("Auto-tunneling: no condition met")
			}
		}
	}
}
