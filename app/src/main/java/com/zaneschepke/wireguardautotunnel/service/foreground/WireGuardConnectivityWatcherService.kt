package com.zaneschepke.wireguardautotunnel.service.foreground

import android.content.Context
import android.os.Bundle
import android.os.PowerManager
import androidx.core.app.ServiceCompat
import androidx.lifecycle.lifecycleScope
import com.zaneschepke.wireguardautotunnel.R
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
import com.zaneschepke.wireguardautotunnel.service.tunnel.TunnelState
import com.zaneschepke.wireguardautotunnel.service.tunnel.VpnService
import com.zaneschepke.wireguardautotunnel.util.Constants
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.net.InetAddress
import javax.inject.Inject


@AndroidEntryPoint
class WireGuardConnectivityWatcherService : ForegroundService() {
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
    lateinit var vpnService: VpnService

    @Inject
    lateinit var serviceManager: ServiceManager

    @Inject
    @IoDispatcher
    lateinit var ioDispatcher: CoroutineDispatcher

    @Inject
    @MainImmediateDispatcher
    lateinit var mainImmediateDispatcher: CoroutineDispatcher

    private val networkEventsFlow = MutableStateFlow(WatcherState())

    private var watcherJob: Job? = null

    private var wakeLock: PowerManager.WakeLock? = null
    private val tag = this.javaClass.name

    override fun onCreate() {
        super.onCreate()
        lifecycleScope.launch(mainImmediateDispatcher) {
            try {
                if (appDataRepository.settings.getSettings().isAutoTunnelPaused) {
                    launchWatcherPausedNotification()
                } else launchWatcherNotification()
            } catch (e: Exception) {
                Timber.e("Failed to start watcher service, not enough permissions")
            }
        }
    }

    override fun startService(extras: Bundle?) {
        super.startService(extras)
        try {
            // we need this lock so our service gets not affected by Doze Mode
            lifecycleScope.launch { initWakeLock() }
            cancelWatcherJob()
            startWatcherJob()
        } catch (e: Exception) {
            Timber.e("Failed to launch watcher service, no permissions")
        }
    }

    override fun stopService() {
        super.stopService()
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
        cancelWatcherJob()
        stopSelf()
    }

    private fun launchWatcherNotification(
        description: String = getString(R.string.watcher_notification_text_active)
    ) {
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

    private fun cancelWatcherJob() {
        try {
            watcherJob?.cancel()
        } catch (e: CancellationException) {
            Timber.i("Watcher job cancelled")
        }
    }

    private fun startWatcherJob() {
        watcherJob =
            lifecycleScope.launch {
                val setting = appDataRepository.settings.getSettings()
                launch {
                    Timber.i("Starting wifi watcher")
                    watchForWifiConnectivityChanges()
                }
                if (setting.isTunnelOnMobileDataEnabled) {
                    launch {
                        Timber.i("Starting mobile data watcher")
                        watchForMobileDataConnectivityChanges()
                    }
                }
                if (setting.isTunnelOnEthernetEnabled) {
                    launch {
                        Timber.i("Starting ethernet data watcher")
                        watchForEthernetConnectivityChanges()
                    }
                }
                launch {
                    Timber.i("Starting settings watcher")
                    watchForSettingsChanges()
                }
                if (setting.isPingEnabled) {
                    launch {
                        Timber.i("Starting ping watcher")
                        watchForPingFailure()
                    }
                }
                launch {
                    Timber.i("Starting management watcher")
                    manageVpn()
                }

            }
    }

    private suspend fun watchForMobileDataConnectivityChanges() {
        withContext(ioDispatcher) {
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
        val context = this
        withContext(ioDispatcher) {
            try {
                do {
                    if (vpnService.vpnState.value.status == TunnelState.UP) {
                        val tunnelConfig = vpnService.vpnState.value.tunnelConfig
                        tunnelConfig?.let {
                            val config = TunnelConfig.configFromWgQuick(it.wgQuick)
                            val results = config.peers.map { peer ->
                                val host = if (peer.endpoint.isPresent &&
                                    peer.endpoint.get().resolved.isPresent)
                                    peer.endpoint.get().resolved.get().host
                                else Constants.DEFAULT_PING_IP
                                Timber.i("Checking reachability of: $host")
                                val reachable = InetAddress.getByName(host)
                                    .isReachable(Constants.PING_TIMEOUT.toInt())
                                Timber.i("Result: reachable - $reachable")
                                reachable
                            }
                            if (results.contains(false)) {
                                Timber.i("Restarting VPN for ping failure")
                                serviceManager.stopVpnServiceForeground(context)
                                delay(Constants.VPN_RESTART_DELAY)
                                serviceManager.startVpnServiceForeground(context, it.id)
                                delay(Constants.PING_COOLDOWN)
                            }
                        }
                    }
                    delay(Constants.PING_INTERVAL)
                } while (true)
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }

    private suspend fun watchForSettingsChanges() {
        appDataRepository.settings.getSettingsFlow().collect { settings ->
            if (networkEventsFlow.value.settings.isAutoTunnelPaused != settings.isAutoTunnelPaused) {
                when (settings.isAutoTunnelPaused) {
                    true -> launchWatcherPausedNotification()
                    false -> launchWatcherNotification()
                }
            }
            networkEventsFlow.update {
                it.copy(
                    settings = settings,
                )
            }
        }
    }

    private suspend fun watchForEthernetConnectivityChanges() {
        withContext(ioDispatcher) {
            ethernetService.networkStatus.collect { status ->
                when (status) {
                    is NetworkStatus.Available -> {
                        Timber.i("Gained Ethernet connection")
                        networkEventsFlow.update {
                            it.copy(
                                isEthernetConnected = true,
                            )
                        }
                    }

                    is NetworkStatus.CapabilitiesChanged -> {
                        Timber.i("Ethernet capabilities changed")
                        networkEventsFlow.update {
                            it.copy(
                                isEthernetConnected = true,
                            )
                        }
                    }

                    is NetworkStatus.Unavailable -> {
                        networkEventsFlow.update {
                            it.copy(
                                isEthernetConnected = false,
                            )
                        }
                        Timber.i("Lost Ethernet connection")
                    }
                }
            }
        }
    }

    private suspend fun watchForWifiConnectivityChanges() {
        withContext(ioDispatcher) {
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
                            } else Timber.i("Detected valid SSID")
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
        return vpnService.vpnState.value.status == TunnelState.DOWN
    }

    private suspend fun manageVpn() {
        val context = this
        withContext(ioDispatcher) {
            networkEventsFlow.collectLatest { watcherState ->
                val autoTunnel = "Auto-tunnel watcher"
                if (!watcherState.settings.isAutoTunnelPaused) {
                    //delay for rapid network state changes and then collect latest
                    delay(Constants.WATCHER_COLLECTION_DELAY)
                    val tunnelConfig = vpnService.vpnState.value.tunnelConfig
                    when {
                        watcherState.isEthernetConditionMet() -> {
                            Timber.i("$autoTunnel - tunnel on on ethernet condition met")
                            if (isTunnelDown()) serviceManager.startVpnServiceForeground(context)
                        }

                        watcherState.isMobileDataConditionMet() -> {
                            Timber.i("$autoTunnel - tunnel on on mobile data condition met")
                            val mobileDataTunnel = getMobileDataTunnel()
                            val tunnel =
                                mobileDataTunnel ?: appDataRepository.getPrimaryOrFirstTunnel()
                            if (isTunnelDown()) return@collectLatest serviceManager.startVpnServiceForeground(
                                context,
                                tunnel?.id,
                            )
                            if (tunnelConfig?.isMobileDataTunnel == false && mobileDataTunnel != null) {
                                Timber.i("$autoTunnel - tunnel connected on mobile data is not preferred condition met, switching to preferred")
                                serviceManager.startVpnServiceForeground(
                                    context,
                                    mobileDataTunnel.id,
                                )
                            }
                        }

                        watcherState.isTunnelOffOnMobileDataConditionMet() -> {
                            Timber.i("$autoTunnel - tunnel off on mobile data met, turning vpn off")
                            if (!isTunnelDown()) serviceManager.stopVpnServiceForeground(context)
                        }

                        watcherState.isUntrustedWifiConditionMet() -> {
                            if (tunnelConfig?.tunnelNetworks?.contains(watcherState.currentNetworkSSID) == false ||
                                tunnelConfig == null) {
                                Timber.i("$autoTunnel - tunnel on ssid not associated with current tunnel condition met")
                                getSsidTunnel(watcherState.currentNetworkSSID)?.let {
                                    Timber.i("Found tunnel associated with this SSID, bringing tunnel up")
                                    if (isTunnelDown()) serviceManager.startVpnServiceForeground(
                                        context,
                                        it.id,
                                    )
                                } ?: suspend {
                                    Timber.i("No tunnel associated with this SSID, using defaults")
                                    val default = appDataRepository.getPrimaryOrFirstTunnel()
                                    if (default?.name != vpnService.name) {
                                        default?.let {
                                            serviceManager.startVpnServiceForeground(context, it.id)
                                        }

                                    }
                                }.invoke()
                            }
                        }

                        watcherState.isTrustedWifiConditionMet() -> {
                            Timber.i("$autoTunnel - tunnel off on trusted wifi condition met, turning vpn off")
                            if (!isTunnelDown()) serviceManager.stopVpnServiceForeground(context)
                        }

                        watcherState.isTunnelOffOnWifiConditionMet() -> {
                            Timber.i("$autoTunnel - tunnel off on wifi condition met, turning vpn off")
                            if (!isTunnelDown()) serviceManager.stopVpnServiceForeground(context)
                        }

                        watcherState.isTunnelOffOnNoConnectivityMet() -> {
                            Timber.i("$autoTunnel - tunnel off on no connectivity met, turning vpn off")
                            if (!isTunnelDown()) serviceManager.stopVpnServiceForeground(context)
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
