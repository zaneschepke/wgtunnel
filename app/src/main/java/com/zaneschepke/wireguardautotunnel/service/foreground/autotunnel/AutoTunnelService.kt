package com.zaneschepke.wireguardautotunnel.service.foreground.autotunnel

import android.content.Intent
import android.net.NetworkCapabilities
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.wireguard.android.util.RootShell
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.data.domain.Settings
import com.zaneschepke.wireguardautotunnel.data.domain.TunnelConfig
import com.zaneschepke.wireguardautotunnel.data.repository.AppDataRepository
import com.zaneschepke.wireguardautotunnel.module.AppShell
import com.zaneschepke.wireguardautotunnel.module.IoDispatcher
import com.zaneschepke.wireguardautotunnel.module.MainImmediateDispatcher
import com.zaneschepke.wireguardautotunnel.service.foreground.ServiceManager
import com.zaneschepke.wireguardautotunnel.service.foreground.autotunnel.model.AutoTunnelEvent
import com.zaneschepke.wireguardautotunnel.service.foreground.autotunnel.model.AutoTunnelState
import com.zaneschepke.wireguardautotunnel.service.foreground.autotunnel.model.NetworkState
import com.zaneschepke.wireguardautotunnel.service.network.EthernetService
import com.zaneschepke.wireguardautotunnel.service.network.MobileDataService
import com.zaneschepke.wireguardautotunnel.service.network.NetworkService
import com.zaneschepke.wireguardautotunnel.service.network.NetworkStatus
import com.zaneschepke.wireguardautotunnel.service.network.WifiService
import com.zaneschepke.wireguardautotunnel.service.notification.NotificationService
import com.zaneschepke.wireguardautotunnel.service.tunnel.TunnelService
import com.zaneschepke.wireguardautotunnel.util.Constants
import com.zaneschepke.wireguardautotunnel.util.extensions.TunnelConfigs
import com.zaneschepke.wireguardautotunnel.util.extensions.cancelWithMessage
import com.zaneschepke.wireguardautotunnel.util.extensions.getCurrentWifiName
import com.zaneschepke.wireguardautotunnel.util.extensions.isReachable
import com.zaneschepke.wireguardautotunnel.util.extensions.onNotRunning
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.net.InetAddress
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Provider

@AndroidEntryPoint
class AutoTunnelService : LifecycleService() {
	private val foregroundId = 122

	@Inject
	@AppShell
	lateinit var rootShell: Provider<RootShell>

	@Inject
	lateinit var wifiService: NetworkService<WifiService>

	@Inject
	lateinit var mobileDataService: NetworkService<MobileDataService>

	@Inject
	lateinit var ethernetService: NetworkService<EthernetService>

	@Inject
	lateinit var appDataRepository: Provider<AppDataRepository>

	@Inject
	lateinit var notificationService: NotificationService

	@Inject
	lateinit var tunnelService: Provider<TunnelService>

	@Inject
	@IoDispatcher
	lateinit var ioDispatcher: CoroutineDispatcher

	@Inject
	lateinit var serviceManager: ServiceManager

	@Inject
	@MainImmediateDispatcher
	lateinit var mainImmediateDispatcher: CoroutineDispatcher

	private val autoTunnelStateFlow = MutableStateFlow(AutoTunnelState())

	private var wakeLock: PowerManager.WakeLock? = null

	private val pingTunnelRestartActive = AtomicBoolean(false)

	private var pingJob: Job? = null

	override fun onCreate() {
		super.onCreate()
		serviceManager.autoTunnelService.complete(this)
		lifecycleScope.launch(mainImmediateDispatcher) {
			kotlin.runCatching {
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
		Timber.d("onStartCommand executed with startId: $startId")
		serviceManager.autoTunnelService.complete(this)
		return super.onStartCommand(intent, flags, startId)
	}

	fun start() {
		kotlin.runCatching {
			lifecycleScope.launch(mainImmediateDispatcher) {
				launchWatcherNotification()
				initWakeLock()
			}
			startAutoTunnelJob()
			startAutoTunnelStateJob()
			startPingStateJob()
		}.onFailure {
			Timber.e(it)
		}
	}

	fun stop() {
		wakeLock?.let { if (it.isHeld) it.release() }
		stopSelf()
	}

	override fun onDestroy() {
		cancelAndResetPingJob()
		serviceManager.autoTunnelService = CompletableDeferred()
		super.onDestroy()
	}

	private fun launchWatcherNotification(description: String = getString(R.string.monitoring_state_changes)) {
		val notification =
			notificationService.createNotification(
				channelId = getString(R.string.watcher_channel_id),
				channelName = getString(R.string.watcher_channel_name),
				title = getString(R.string.auto_tunnel_title),
				description = description,
			)
		ServiceCompat.startForeground(
			this,
			foregroundId,
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

	private fun startPingJob() = lifecycleScope.launch {
		watchForPingFailure()
	}

	private fun startPingStateJob() = lifecycleScope.launch {
		autoTunnelStateFlow.collect {
			if (it.isPingEnabled()) {
				pingJob.onNotRunning { pingJob = startPingJob() }
			} else {
				if (!pingTunnelRestartActive.get()) cancelAndResetPingJob()
			}
		}
	}

	private suspend fun watchForPingFailure() {
		withContext(ioDispatcher) {
			Timber.i("Starting ping watcher")
			runCatching {
				do {
					val vpnState = autoTunnelStateFlow.value.vpnState
					if (vpnState.status.isUp() && !autoTunnelStateFlow.value.isNoConnectivity()) {
						if (vpnState.tunnelConfig != null) {
							val config = TunnelConfig.configFromWgQuick(vpnState.tunnelConfig.wgQuick)
							val results = if (vpnState.tunnelConfig.pingIp != null) {
								Timber.d("Pinging custom ip : ${vpnState.tunnelConfig.pingIp}")
								listOf(InetAddress.getByName(vpnState.tunnelConfig.pingIp).isReachable(Constants.PING_TIMEOUT.toInt()))
							} else {
								Timber.d("Pinging all peers")
								config.peers.map { peer ->
									peer.isReachable()
								}
							}
							Timber.i("Ping results reachable: $results")
							if (results.contains(false)) {
								Timber.i("Restarting VPN for ping failure")
								val cooldown = vpnState.tunnelConfig.pingCooldown
								pingTunnelRestartActive.set(true)
								tunnelService.get().bounceTunnel()
								pingTunnelRestartActive.set(false)
								delay(cooldown ?: Constants.PING_COOLDOWN)
								continue
							}
						}
					}
					delay(vpnState.tunnelConfig?.pingInterval ?: Constants.PING_INTERVAL)
				} while (true)
			}.onFailure {
				Timber.e(it)
			}
		}
	}

	private fun startAutoTunnelStateJob() = lifecycleScope.launch(ioDispatcher) {
		combine(
			combineSettings(),
			combineNetworkEventsJob(),
		) { double, networkState ->
			AutoTunnelState(tunnelService.get().vpnState.value, networkState, double.first, double.second)
		}.collect { state ->
			autoTunnelStateFlow.update {
				it.copy(state.vpnState, state.networkState, state.settings, state.tunnels)
			}
		}
	}

	private fun cancelAndResetPingJob() {
		pingJob?.cancelWithMessage("Ping job canceled")
		pingJob = null
	}

	private fun combineNetworkEventsJob(): Flow<NetworkState> {
		return combine(
			wifiService.networkStatus,
			mobileDataService.networkStatus,
			ethernetService.networkStatus,
		) { wifi, mobileData, ethernet ->
			NetworkState(
				wifi.isConnected,
				mobileData.isConnected,
				ethernet.isConnected,
				when (wifi) {
					is NetworkStatus.CapabilitiesChanged -> getWifiSSID(wifi.networkCapabilities)
					is NetworkStatus.Available -> autoTunnelStateFlow.value.networkState.wifiName
					is NetworkStatus.Unavailable -> null
				},
			)
		}.distinctUntilChanged()
	}

	private fun combineSettings(): Flow<Pair<Settings, TunnelConfigs>> {
		return combine(
			appDataRepository.get().settings.getSettingsFlow(),
			appDataRepository.get().tunnels.getTunnelConfigsFlow().distinctUntilChanged { old, new ->
				old.map { it.isActive } != new.map { it.isActive }
			},
		) { settings, tunnels ->
			Pair(settings, tunnels)
		}.distinctUntilChanged()
	}

	private suspend fun getWifiSSID(networkCapabilities: NetworkCapabilities): String? {
		return withContext(ioDispatcher) {
			with(autoTunnelStateFlow.value.settings) {
				if (isWifiNameByShellEnabled) return@withContext rootShell.get().getCurrentWifiName()
				wifiService.getNetworkName(networkCapabilities)
			}.also {
				if (it?.contains(Constants.UNREADABLE_SSID) == true) {
					Timber.w("SSID unreadable: missing permissions")
				} else {
					Timber.i("Detected valid SSID")
				}
			}
		}
	}

	private fun startAutoTunnelJob() = lifecycleScope.launch(ioDispatcher) {
		Timber.i("Starting auto-tunnel network event watcher")
		autoTunnelStateFlow.collect { watcherState ->
			Timber.d("New auto tunnel state emitted")
			when (val event = watcherState.asAutoTunnelEvent()) {
				is AutoTunnelEvent.Start -> tunnelService.get().startTunnel(
					event.tunnelConfig
						?: appDataRepository.get().getPrimaryOrFirstTunnel(),
				)
				is AutoTunnelEvent.Stop -> tunnelService.get().stopTunnel()
				AutoTunnelEvent.DoNothing -> Timber.i("Auto-tunneling: no condition met")
			}
		}
	}
}
