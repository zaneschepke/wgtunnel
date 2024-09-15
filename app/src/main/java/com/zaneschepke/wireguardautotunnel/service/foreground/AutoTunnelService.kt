package com.zaneschepke.wireguardautotunnel.service.foreground

import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.data.domain.Settings
import com.zaneschepke.wireguardautotunnel.data.domain.TunnelConfig
import com.zaneschepke.wireguardautotunnel.data.repository.AppDataRepository
import com.zaneschepke.wireguardautotunnel.module.IoDispatcher
import com.zaneschepke.wireguardautotunnel.module.MainImmediateDispatcher
import com.zaneschepke.wireguardautotunnel.service.network.EthernetService
import com.zaneschepke.wireguardautotunnel.service.network.MobileDataService
import com.zaneschepke.wireguardautotunnel.service.network.NetworkService
import com.zaneschepke.wireguardautotunnel.service.network.NetworkStatus
import com.zaneschepke.wireguardautotunnel.service.network.WifiService
import com.zaneschepke.wireguardautotunnel.service.notification.NotificationService
import com.zaneschepke.wireguardautotunnel.service.tunnel.TunnelService
import com.zaneschepke.wireguardautotunnel.service.tunnel.TunnelState
import com.zaneschepke.wireguardautotunnel.util.Constants
import com.zaneschepke.wireguardautotunnel.util.extensions.cancelWithMessage
import com.zaneschepke.wireguardautotunnel.util.extensions.isMatchingToWildcardList
import com.zaneschepke.wireguardautotunnel.util.extensions.isReachable
import com.zaneschepke.wireguardautotunnel.util.extensions.onNotRunning
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.net.InetAddress
import javax.inject.Inject
import javax.inject.Provider

@AndroidEntryPoint
class AutoTunnelService : LifecycleService() {
	private val foregroundId = 122

	@Inject
	lateinit var wifiService: NetworkService<WifiService>

	@Inject
	lateinit var mobileDataService: NetworkService<MobileDataService>

	@Inject
	lateinit var ethernetService: NetworkService<EthernetService>

	@Inject
	lateinit var appDataRepository: AppDataRepository

	@Inject
	lateinit var notificationService: NotificationService

	@Inject
	lateinit var tunnelService: Provider<TunnelService>

	@Inject
	@IoDispatcher
	lateinit var ioDispatcher: CoroutineDispatcher

	@Inject
	@MainImmediateDispatcher
	lateinit var mainImmediateDispatcher: CoroutineDispatcher

	private val networkEventsFlow = MutableStateFlow(AutoTunnelState())

	private var wakeLock: PowerManager.WakeLock? = null

	private var wifiJob: Job? = null
	private var mobileDataJob: Job? = null
	private var ethernetJob: Job? = null
	private var pingJob: Job? = null
	private var networkEventJob: Job? = null

	@get:Synchronized @set:Synchronized
	private var running: Boolean = false

	override fun onCreate() {
		super.onCreate()
		lifecycleScope.launch(mainImmediateDispatcher) {
			kotlin.runCatching {
				launchNotification()
			}.onFailure {
				Timber.e(it)
			}
		}
	}

	override fun onBind(intent: Intent): IBinder? {
		super.onBind(intent)
		// We don't provide binding, so return null
		return null
	}

	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
		Timber.d("onStartCommand executed with startId: $startId")
		if (intent != null) {
			val action = intent.action
			when (action) {
				Action.START.name,
				Action.START_FOREGROUND.name,
				-> startService()
				Action.STOP.name, Action.STOP_FOREGROUND.name -> stopService()
			}
		}
		return super.onStartCommand(intent, flags, startId)
	}

	private suspend fun launchNotification() {
		if (appDataRepository.settings.getSettings().isAutoTunnelPaused) {
			launchWatcherPausedNotification()
		} else {
			launchWatcherNotification()
		}
	}

	private fun startService() {
		if (running) return
		running = true
		kotlin.runCatching {
			lifecycleScope.launch(mainImmediateDispatcher) {
				launchNotification()
				initWakeLock()
			}
			startSettingsJob()
		}.onFailure {
			Timber.e(it)
		}
	}

	private fun stopService() {
		wakeLock?.let {
			if (it.isHeld) {
				it.release()
			}
		}
		stopSelf()
	}

	override fun onDestroy() {
		cancelAndResetNetworkJobs()
		cancelAndResetPingJob()
		super.onDestroy()
	}

	private fun launchWatcherNotification(description: String = getString(R.string.watcher_notification_text_active)) {
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

	private fun launchWatcherPausedNotification() {
		launchWatcherNotification(getString(R.string.watcher_notification_text_paused))
	}

	private fun initWakeLock() {
		wakeLock =
			(getSystemService(Context.POWER_SERVICE) as PowerManager).run {
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

	private fun startSettingsJob() = lifecycleScope.launch {
		watchForSettingsChanges()
	}

	private fun startWifiJob() = lifecycleScope.launch {
		watchForWifiConnectivityChanges()
	}

	private fun startMobileDataJob() = lifecycleScope.launch {
		watchForMobileDataConnectivityChanges()
	}

	private fun startEthernetJob() = lifecycleScope.launch {
		watchForEthernetConnectivityChanges()
	}

	private fun startPingJob() = lifecycleScope.launch {
		watchForPingFailure()
	}

	private fun startNetworkEventJob() = lifecycleScope.launch {
		handleNetworkEventChanges()
	}

	private suspend fun watchForMobileDataConnectivityChanges() {
		withContext(ioDispatcher) {
			Timber.i("Starting mobile data watcher")
			mobileDataService.networkStatus.collect { status ->
				when (status) {
					is NetworkStatus.Available -> {
						Timber.i("Gained Mobile data connection")
						networkEventsFlow.update {
							it.copy(
								isMobileDataConnected = true,
							)
						}
					}

					is NetworkStatus.CapabilitiesChanged -> {
						networkEventsFlow.update {
							it.copy(
								isMobileDataConnected = true,
							)
						}
						Timber.i("Mobile data capabilities changed")
					}

					is NetworkStatus.Unavailable -> {
						networkEventsFlow.update {
							it.copy(
								isMobileDataConnected = false,
							)
						}
						Timber.i("Lost mobile data connection")
					}
				}
			}
		}
	}

	private suspend fun watchForPingFailure() {
		withContext(ioDispatcher) {
			Timber.i("Starting ping watcher")
			runCatching {
				do {
					val vpnState = tunnelService.get().vpnState.value
					if (vpnState.status == TunnelState.UP) {
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
								tunnelService.get().bounceTunnel(vpnState.tunnelConfig)
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

	private fun updateSettings(settings: Settings) {
		networkEventsFlow.update {
			it.copy(
				settings = settings,
			)
		}
	}

	private fun onAutoTunnelPause(paused: Boolean) {
		if (networkEventsFlow.value.settings.isAutoTunnelPaused
			!= paused
		) {
			when (paused) {
				true -> launchWatcherPausedNotification()
				false -> launchWatcherNotification()
			}
		}
	}

	private suspend fun watchForSettingsChanges() {
		Timber.i("Starting settings watcher")
		withContext(ioDispatcher) {
			appDataRepository.settings.getSettingsFlow().combine(
				appDataRepository.tunnels.getTunnelConfigsFlow(),
			) { settings, tunnels ->
				val activeTunnel = tunnels.firstOrNull { it.isActive }
				if (!settings.isPingEnabled) {
					settings.copy(isPingEnabled = activeTunnel?.isPingEnabled ?: false)
				} else {
					settings
				}
			}.collect {
				Timber.d("Settings change: $it")
				onAutoTunnelPause(it.isAutoTunnelPaused)
				updateSettings(it)
				manageJobsBySettings(it)
			}
		}
	}

	private fun manageJobsBySettings(settings: Settings) {
		with(settings) {
			if (isPingEnabled) {
				pingJob.onNotRunning { pingJob = startPingJob() }
			} else {
				cancelAndResetPingJob()
			}
			if (isTunnelOnWifiEnabled || isTunnelOnEthernetEnabled || isTunnelOnMobileDataEnabled) {
				startNetworkJobs()
			} else {
				cancelAndResetNetworkJobs()
			}
		}
	}

	private fun startNetworkJobs() {
		wifiJob.onNotRunning {
			Timber.i("Wifi job starting")
			wifiJob = startWifiJob()
		}
		ethernetJob.onNotRunning {
			ethernetJob = startEthernetJob()
			Timber.i("Ethernet job starting")
		}
		mobileDataJob.onNotRunning {
			mobileDataJob = startMobileDataJob()
			Timber.i("Mobile data job starting")
		}
		networkEventJob.onNotRunning {
			Timber.i("Network event job starting")
			networkEventJob = startNetworkEventJob()
		}
	}

	private fun cancelAndResetPingJob() {
		pingJob?.cancelWithMessage("Ping job canceled")
		pingJob = null
	}

	private fun cancelAndResetNetworkJobs() {
		networkEventJob?.cancelWithMessage("Network event job canceled")
		wifiJob?.cancelWithMessage("Wifi job canceled")
		ethernetJob?.cancelWithMessage("Ethernet job canceled")
		mobileDataJob?.cancelWithMessage("Mobile data job canceled")
		networkEventJob = null
		wifiJob = null
		ethernetJob = null
		mobileDataJob = null
	}

	private fun updateEthernet(connected: Boolean) {
		networkEventsFlow.update {
			it.copy(
				isEthernetConnected = connected,
			)
		}
	}

	private suspend fun watchForEthernetConnectivityChanges() {
		withContext(ioDispatcher) {
			Timber.i("Starting ethernet data watcher")
			ethernetService.networkStatus.collect { status ->
				when (status) {
					is NetworkStatus.Available -> {
						Timber.i("Gained Ethernet connection")
						updateEthernet(true)
					}

					is NetworkStatus.CapabilitiesChanged -> {
						Timber.i("Ethernet capabilities changed")
						updateEthernet(true)
					}

					is NetworkStatus.Unavailable -> {
						updateEthernet(false)
						Timber.i("Lost Ethernet connection")
					}
				}
			}
		}
	}

	private suspend fun watchForWifiConnectivityChanges() {
		withContext(ioDispatcher) {
			Timber.i("Starting wifi watcher")
			wifiService.networkStatus.collect { status ->
				when (status) {
					is NetworkStatus.Available -> {
						Timber.i("Gained Wi-Fi connection")
						networkEventsFlow.update {
							it.copy(
								isWifiConnected = true,
							)
						}
					}

					is NetworkStatus.CapabilitiesChanged -> {
						Timber.i("Wifi capabilities changed")
						networkEventsFlow.update {
							it.copy(
								isWifiConnected = true,
							)
						}
						val ssid = wifiService.getNetworkName(status.networkCapabilities)
						ssid?.let { name ->
							if (name.contains(Constants.UNREADABLE_SSID)) {
								Timber.w("SSID unreadable: missing permissions")
							} else {
								Timber.i("Detected valid SSID")
							}
							appDataRepository.appState.setCurrentSsid(name)
							networkEventsFlow.update {
								it.copy(
									currentNetworkSSID = name,
								)
							}
						} ?: Timber.w("Failed to read ssid")
					}

					is NetworkStatus.Unavailable -> {
						networkEventsFlow.update {
							it.copy(
								isWifiConnected = false,
							)
						}
						Timber.i("Lost Wi-Fi connection")
					}
				}
			}
		}
	}

	private suspend fun getMobileDataTunnel(): TunnelConfig? {
		return appDataRepository.tunnels.findByMobileDataTunnel().firstOrNull()
	}

	private suspend fun getSsidTunnel(ssid: String): TunnelConfig? {
		return appDataRepository.tunnels.findByTunnelNetworksName(ssid).firstOrNull()
	}

	private fun isTunnelDown(): Boolean {
		return tunnelService.get().vpnState.value.status == TunnelState.DOWN
	}

	private suspend fun handleNetworkEventChanges() {
		withContext(ioDispatcher) {
			Timber.i("Starting network event watcher")
			networkEventsFlow.collectLatest { watcherState ->
				val autoTunnel = "Auto-tunnel watcher"
				if (!watcherState.settings.isAutoTunnelPaused) {
					// delay for rapid network state changes and then collect latest
					delay(Constants.WATCHER_COLLECTION_DELAY)
					val activeTunnel = tunnelService.get().vpnState.value.tunnelConfig
					val defaultTunnel = appDataRepository.getPrimaryOrFirstTunnel()
					when {
						watcherState.isEthernetConditionMet() -> {
							Timber.i("$autoTunnel - tunnel on on ethernet condition met")
							if (isTunnelDown()) {
								defaultTunnel?.let {
									tunnelService.get().startTunnel(it)
								}
							}
						}

						watcherState.isMobileDataConditionMet() -> {
							Timber.i("$autoTunnel - tunnel on mobile data condition met")
							val mobileDataTunnel = getMobileDataTunnel()
							val tunnel =
								mobileDataTunnel ?: defaultTunnel
							if (isTunnelDown() || activeTunnel?.isMobileDataTunnel == false) {
								tunnel?.let {
									tunnelService.get().startTunnel(it)
								}
							}
						}

						watcherState.isTunnelOffOnMobileDataConditionMet() -> {
							Timber.i("$autoTunnel - tunnel off on mobile data met, turning vpn off")
							if (!isTunnelDown()) {
								activeTunnel?.let {
									tunnelService.get().stopTunnel(it)
								}
							}
						}

						watcherState.isUntrustedWifiConditionMet() -> {
							Timber.i("Untrusted wifi condition met")
							if (activeTunnel?.tunnelNetworks?.isMatchingToWildcardList(watcherState.currentNetworkSSID) == false ||
								activeTunnel == null || isTunnelDown()
							) {
								Timber.i(
									"$autoTunnel - tunnel on ssid not associated with current tunnel condition met",
								)
								getSsidTunnel(watcherState.currentNetworkSSID)?.let {
									Timber.i("Found tunnel associated with this SSID, bringing tunnel up: ${it.name}")
									if (isTunnelDown() || activeTunnel?.id != it.id) {
										tunnelService.get().startTunnel(it)
									}
								} ?: suspend {
									Timber.i("No tunnel associated with this SSID, using defaults")
									val default = appDataRepository.getPrimaryOrFirstTunnel()
									if (default?.name != tunnelService.get().name || isTunnelDown()) {
										default?.let {
											tunnelService.get().startTunnel(it)
										}
									}
								}.invoke()
							}
						}

						watcherState.isTrustedWifiConditionMet() -> {
							Timber.i(
								"$autoTunnel - tunnel off on trusted wifi condition met, turning vpn off",
							)
							if (!isTunnelDown()) activeTunnel?.let { tunnelService.get().stopTunnel(it) }
						}

						watcherState.isTunnelOffOnWifiConditionMet() -> {
							Timber.i(
								"$autoTunnel - tunnel off on wifi condition met, turning vpn off",
							)
							if (!isTunnelDown()) activeTunnel?.let { tunnelService.get().stopTunnel(it) }
						}

						watcherState.isTunnelOffOnNoConnectivityMet() -> {
							Timber.i(
								"$autoTunnel - tunnel off on no connectivity met, turning vpn off",
							)
							if (!isTunnelDown()) activeTunnel?.let { tunnelService.get().stopTunnel(it) }
						}

						else -> {
							Timber.i("$autoTunnel - no condition met")
						}
					}
				}
			}
		}
	}
}
