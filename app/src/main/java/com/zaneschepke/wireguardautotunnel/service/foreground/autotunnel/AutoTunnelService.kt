package com.zaneschepke.wireguardautotunnel.service.foreground.autotunnel

import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.wireguard.android.util.RootShell
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.data.domain.Settings
import com.zaneschepke.wireguardautotunnel.data.repository.AppDataRepository
import com.zaneschepke.wireguardautotunnel.module.AppShell
import com.zaneschepke.wireguardautotunnel.module.Ethernet
import com.zaneschepke.wireguardautotunnel.module.IoDispatcher
import com.zaneschepke.wireguardautotunnel.module.MainImmediateDispatcher
import com.zaneschepke.wireguardautotunnel.module.MobileData
import com.zaneschepke.wireguardautotunnel.module.Wifi
import com.zaneschepke.wireguardautotunnel.service.foreground.ServiceManager
import com.zaneschepke.wireguardautotunnel.service.foreground.autotunnel.model.AutoTunnelEvent
import com.zaneschepke.wireguardautotunnel.service.foreground.autotunnel.model.AutoTunnelState
import com.zaneschepke.wireguardautotunnel.service.foreground.autotunnel.model.NetworkState
import com.zaneschepke.wireguardautotunnel.service.network.NetworkService
import com.zaneschepke.wireguardautotunnel.service.network.WifiService
import com.zaneschepke.wireguardautotunnel.service.notification.NotificationAction
import com.zaneschepke.wireguardautotunnel.service.notification.NotificationService
import com.zaneschepke.wireguardautotunnel.service.notification.WireGuardNotification
import com.zaneschepke.wireguardautotunnel.service.tunnel.TunnelService
import com.zaneschepke.wireguardautotunnel.util.Constants
import com.zaneschepke.wireguardautotunnel.util.extensions.TunnelConfigs
import com.zaneschepke.wireguardautotunnel.util.extensions.getCurrentWifiName
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Provider

@AndroidEntryPoint
class AutoTunnelService : LifecycleService() {

	@Inject
	@AppShell
	lateinit var rootShell: Provider<RootShell>

	@Inject
	@Wifi
	lateinit var wifiService: NetworkService

	@Inject
	@MobileData
	lateinit var mobileDataService: NetworkService

	@Inject
	@Ethernet
	lateinit var ethernetService: NetworkService

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

	private val defaultState = AutoTunnelState()

	private val autoTunnelStateFlow = MutableStateFlow(defaultState)

	private var wakeLock: PowerManager.WakeLock? = null

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
		super.onDestroy()
	}

	private fun launchWatcherNotification(description: String = getString(R.string.monitoring_state_changes)) {
		val notification =
			notificationService.createNotification(
				WireGuardNotification.NotificationChannels.AUTO_TUNNEL,
				title = getString(R.string.auto_tunnel_title),
				description = description,
				actions = listOf(
					notificationService.createNotificationAction(NotificationAction.AUTO_TUNNEL_OFF),
				),
			)
		ServiceCompat.startForeground(
			this,
			NotificationService.AUTO_TUNNEL_NOTIFICATION_ID,
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

	private fun startAutoTunnelStateJob() = lifecycleScope.launch(ioDispatcher) {
		combine(
			combineSettings(),
			combineNetworkEventsJob(),
		) { double, networkState ->
			// quick fix for bug where when first setting up auto tunneling we probably want to query for ssid right away
			var netState: NetworkState? = null
			if (networkState.wifiName == Constants.UNREADABLE_SSID && double.first.isTunnelOnWifiEnabled) {
				if (double.first.isWifiNameByShellEnabled) {
					netState = networkState.copy(wifiName = rootShell.get().getCurrentWifiName())
				} else if (networkState.capabilities != null) {
					netState = networkState.copy(
						wifiName =
						WifiService.getNetworkName(networkState.capabilities, this@AutoTunnelService),
					)
				}
			}
			AutoTunnelState(tunnelService.get().vpnState.value, netState ?: networkState, double.first, double.second)
		}.collect { state ->
			Timber.d("Network state: ${state.networkState}")
			autoTunnelStateFlow.update {
				it.copy(vpnState = state.vpnState, networkState = state.networkState, settings = state.settings, tunnels = state.tunnels)
			}
		}
	}

	@OptIn(FlowPreview::class)
	private fun combineNetworkEventsJob(): Flow<NetworkState> {
		return combine(
			wifiService.status,
			mobileDataService.status,
		) { wifi, mobileData ->
			NetworkState(
				wifi.available,
				mobileData.available,
				false,
				wifi.name,
				wifi.capabilities,
			)
		}.distinctUntilChanged()
	}

	private fun combineSettings(): Flow<Pair<Settings, TunnelConfigs>> {
		return combine(
			appDataRepository.get().settings.getSettingsFlow(),
			appDataRepository.get().tunnels.getTunnelConfigsFlow().map { tunnels ->
				// isActive is ignored for equality checks so user can manually toggle off tunnel with auto-tunnel
				tunnels.map { it.copy(isActive = false) }
			},
		) { settings, tunnels ->
			Pair(settings, tunnels)
		}.distinctUntilChanged()
	}

	@OptIn(FlowPreview::class)
	private fun startAutoTunnelJob() = lifecycleScope.launch(ioDispatcher) {
		Timber.i("Starting auto-tunnel network event watcher")
		val settings = appDataRepository.get().settings.getSettings()
		val debounce = settings.debounceDelaySeconds * 1000L
		Timber.d("Starting with debounce delay of: $debounce")
		autoTunnelStateFlow.debounce(debounce).collect { watcherState ->
			if (watcherState == defaultState) return@collect
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
