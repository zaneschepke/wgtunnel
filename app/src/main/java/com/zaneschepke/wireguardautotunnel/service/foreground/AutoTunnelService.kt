package com.zaneschepke.wireguardautotunnel.service.foreground

import android.content.Context
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
import com.zaneschepke.wireguardautotunnel.util.extensions.getCurrentWifiName
import com.zaneschepke.wireguardautotunnel.util.extensions.isDown
import com.zaneschepke.wireguardautotunnel.util.extensions.isReachable
import com.zaneschepke.wireguardautotunnel.util.extensions.onNotRunning
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
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
	lateinit var rootShell: Provider<RootShell>

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
	lateinit var serviceManager: ServiceManager

	@Inject
	@MainImmediateDispatcher
	lateinit var mainImmediateDispatcher: CoroutineDispatcher

	private val autoTunnelStateFlow = MutableStateFlow(AutoTunnelState())

	private var wakeLock: PowerManager.WakeLock? = null

	private var wifiJob: Job? = null
	private var mobileDataJob: Job? = null
	private var ethernetJob: Job? = null
	private var pingJob: Job? = null
	private var networkEventJob: Job? = null

	override fun onCreate() {
		super.onCreate()
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
		// We don't provide binding, so return null
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
			startSettingsJob()
			startVpnStateJob()
		}.onFailure {
			Timber.e(it)
		}
	}

	fun stop() {
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
		serviceManager.autoTunnelService = CompletableDeferred()
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

	private fun startVpnStateJob() = lifecycleScope.launch {
		watchForVpnStateChanges()
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
						autoTunnelStateFlow.update {
							it.copy(
								isMobileDataConnected = true,
							)
						}
					}

					is NetworkStatus.CapabilitiesChanged -> {
						autoTunnelStateFlow.update {
							it.copy(
								isMobileDataConnected = true,
							)
						}
						Timber.i("Mobile data capabilities changed")
					}

					is NetworkStatus.Unavailable -> {
						autoTunnelStateFlow.update {
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

	private suspend fun watchForSettingsChanges() {
		Timber.i("Starting settings watcher")
		withContext(ioDispatcher) {
			appDataRepository.settings.getSettingsFlow().combine(
				// ignore isActive changes to allow manual tunnel overrides
				appDataRepository.tunnels.getTunnelConfigsFlow().distinctUntilChanged { old, new ->
					old.map { it.isActive } != new.map { it.isActive }
				},
			) { settings, tunnels ->
				autoTunnelStateFlow.value.copy(
					settings = settings,
					tunnels = tunnels,
				)
			}.collect {
				Timber.d("got new settings: ${it.settings}")
				manageJobsBySettings(it.settings)
				autoTunnelStateFlow.emit(it)
			}
		}
	}

	private suspend fun watchForVpnStateChanges() {
		Timber.i("Starting vpn state watcher")
		withContext(ioDispatcher) {
			tunnelService.get().vpnState.distinctUntilChanged { old, new ->
				old.tunnelConfig == new.tunnelConfig && old.status == new.status
			}.collect { state ->
				autoTunnelStateFlow.update {
					it.copy(vpnState = state)
				}
				state.tunnelConfig?.let {
					val settings = appDataRepository.settings.getSettings()
					if (it.isPingEnabled && !settings.isPingEnabled) {
						pingJob.onNotRunning { pingJob = startPingJob() }
					}
					if (!it.isPingEnabled && !settings.isPingEnabled) {
						cancelAndResetPingJob()
					}
				}
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
		autoTunnelStateFlow.update {
			it.copy(
				isEthernetConnected = connected,
			)
		}
	}

	private fun updateWifi(connected: Boolean) {
		autoTunnelStateFlow.update {
			it.copy(
				isWifiConnected = connected,
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
						updateWifi(true)
					}

					is NetworkStatus.CapabilitiesChanged -> {
						Timber.i("Wifi capabilities changed")
						updateWifi(true)
						val ssid = getWifiSSID(status.networkCapabilities)
						ssid?.let { name ->
							if (name.contains(Constants.UNREADABLE_SSID)) {
								Timber.w("SSID unreadable: missing permissions")
							} else {
								Timber.i("Detected valid SSID")
							}
							appDataRepository.appState.setCurrentSsid(name)
							autoTunnelStateFlow.update {
								it.copy(
									currentNetworkSSID = name,
								)
							}
						} ?: Timber.w("Failed to read ssid")
					}

					is NetworkStatus.Unavailable -> {
						updateWifi(false)
						Timber.i("Lost Wi-Fi connection")
					}
				}
			}
		}
	}

	private suspend fun getWifiSSID(networkCapabilities: NetworkCapabilities): String? {
		return withContext(ioDispatcher) {
			with(autoTunnelStateFlow.value.settings) {
				if (isWifiNameByShellEnabled) return@withContext rootShell.get().getCurrentWifiName()
				wifiService.getNetworkName(networkCapabilities)
			}
		}
	}

	private suspend fun getMobileDataTunnel(): TunnelConfig? {
		return appDataRepository.tunnels.findByMobileDataTunnel().firstOrNull()
	}

	private suspend fun handleNetworkEventChanges() {
		withContext(ioDispatcher) {
			Timber.i("Starting network event watcher")
			autoTunnelStateFlow.collectLatest { watcherState ->
				val autoTunnel = "Auto-tunnel watcher"
				Timber.d("New watcher state!")
				// delay for rapid network state changes and then collect latest
				delay(Constants.WATCHER_COLLECTION_DELAY)
				val activeTunnel = watcherState.vpnState.tunnelConfig
				val defaultTunnel = appDataRepository.getPrimaryOrFirstTunnel()
				when {
					watcherState.isEthernetConditionMet() -> {
						Timber.i("$autoTunnel - tunnel on on ethernet condition met")
						if (watcherState.vpnState.isDown()) {
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
						if (watcherState.vpnState.isDown() || activeTunnel?.isMobileDataTunnel == false) {
							tunnel?.let {
								tunnelService.get().startTunnel(it)
							}
						}
					}

					watcherState.isTunnelOffOnMobileDataConditionMet() -> {
						Timber.i("$autoTunnel - tunnel off on mobile data met, turning vpn off")
						if (!watcherState.vpnState.isDown()) {
							activeTunnel?.let {
								tunnelService.get().stopTunnel(it)
							}
						}
					}

					watcherState.isUntrustedWifiConditionMet() -> {
						Timber.i("Untrusted wifi condition met")
						if (activeTunnel == null || watcherState.isCurrentSSIDActiveTunnelNetwork() == false ||
							watcherState.vpnState.isDown()
						) {
							Timber.i(
								"$autoTunnel - tunnel on ssid not associated with current tunnel condition met",
							)
							watcherState.getTunnelWithMatchingTunnelNetwork()?.let {
								Timber.i("Found tunnel associated with this SSID, bringing tunnel up: ${it.name}")
								if (watcherState.vpnState.isDown() || activeTunnel?.id != it.id) {
									tunnelService.get().startTunnel(it)
								}
							} ?: suspend {
								Timber.i("No tunnel associated with this SSID, using defaults")
								val default = appDataRepository.getPrimaryOrFirstTunnel()
								if (default?.name != tunnelService.get().name || watcherState.vpnState.isDown()) {
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
						if (!watcherState.vpnState.isDown()) activeTunnel?.let { tunnelService.get().stopTunnel(it) }
					}

					watcherState.isTunnelOffOnWifiConditionMet() -> {
						Timber.i(
							"$autoTunnel - tunnel off on wifi condition met, turning vpn off",
						)
						if (!watcherState.vpnState.isDown()) activeTunnel?.let { tunnelService.get().stopTunnel(it) }
					}

					watcherState.isTunnelOffOnNoConnectivityMet() -> {
						Timber.i(
							"$autoTunnel - tunnel off on no connectivity met, turning vpn off",
						)
						if (!watcherState.vpnState.isDown()) activeTunnel?.let { tunnelService.get().stopTunnel(it) }
					}

					else -> {
						Timber.i("$autoTunnel - no condition met")
					}
				}
			}
		}
	}
}
