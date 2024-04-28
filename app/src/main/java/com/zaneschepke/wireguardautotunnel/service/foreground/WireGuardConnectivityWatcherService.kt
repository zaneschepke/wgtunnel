package com.zaneschepke.wireguardautotunnel.service.foreground

import android.content.Context
import android.os.Bundle
import android.os.PowerManager
import androidx.core.app.ServiceCompat
import androidx.lifecycle.lifecycleScope
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.data.model.TunnelConfig
import com.zaneschepke.wireguardautotunnel.data.repository.AppDataRepository
import com.zaneschepke.wireguardautotunnel.service.network.EthernetService
import com.zaneschepke.wireguardautotunnel.service.network.MobileDataService
import com.zaneschepke.wireguardautotunnel.service.network.NetworkService
import com.zaneschepke.wireguardautotunnel.service.network.NetworkStatus
import com.zaneschepke.wireguardautotunnel.service.network.WifiService
import com.zaneschepke.wireguardautotunnel.service.notification.NotificationService
import com.zaneschepke.wireguardautotunnel.service.tunnel.VpnService
import com.zaneschepke.wireguardautotunnel.util.Constants
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.amnezia.awg.backend.Tunnel
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

    private val networkEventsFlow = MutableStateFlow(WatcherState())

    private var watcherJob: Job? = null

    private var wakeLock: PowerManager.WakeLock? = null
    private val tag = this.javaClass.name

    override fun onCreate() {
        super.onCreate()
        lifecycleScope.launch(Dispatchers.Main) {
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
        } catch (e : CancellationException) {
            Timber.i("Watcher job cancelled")
        }
    }

    private fun startWatcherJob() {
        watcherJob =
            lifecycleScope.launch(Dispatchers.IO) {
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
                    Timber.i("Starting vpn state watcher")
                    watchForVpnConnectivityChanges()
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
        mobileDataService.networkStatus.collect {
            when (it) {
                is NetworkStatus.Available -> {
                    Timber.i("Gained Mobile data connection")
                    networkEventsFlow.value =
                        networkEventsFlow.value.copy(
                            isMobileDataConnected = true,
                        )
                }

                is NetworkStatus.CapabilitiesChanged -> {
                    networkEventsFlow.value =
                        networkEventsFlow.value.copy(
                            isMobileDataConnected = true,
                        )
                    Timber.i("Mobile data capabilities changed")
                }

                is NetworkStatus.Unavailable -> {
                    networkEventsFlow.value =
                        networkEventsFlow.value.copy(
                            isMobileDataConnected = false,
                        )
                    Timber.i("Lost mobile data connection")
                }
            }
        }
    }

    private suspend fun watchForPingFailure() {
        try {
            do {
                if (vpnService.vpnState.value.status == Tunnel.State.UP) {
                    val tunnelConfig = vpnService.vpnState.value.tunnelConfig
                    tunnelConfig?.let {
                        val config = TunnelConfig.configFromQuick(it.wgQuick)
                        val results = config.peers.map { peer ->
                            val host = if (peer.endpoint.isPresent &&
                                peer.endpoint.get().resolved.isPresent)
                                peer.endpoint.get().resolved.get().host
                            else Constants.BACKUP_PING_HOST
                            Timber.i("Checking reachability of: $host")
                            val reachable = InetAddress.getByName(host)
                                .isReachable(Constants.PING_TIMEOUT.toInt())
                            Timber.i("Result: reachable - $reachable")
                            reachable
                        }
                        if (results.contains(false)) {
                            Timber.i("Restarting VPN for ping failure")
                            serviceManager.stopVpnServiceForeground(this)
                            delay(Constants.VPN_RESTART_DELAY)
                            serviceManager.startVpnServiceForeground(this)
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

    private suspend fun watchForSettingsChanges() {
        appDataRepository.settings.getSettingsFlow().collect {
            if (networkEventsFlow.value.settings.isAutoTunnelPaused != it.isAutoTunnelPaused) {
                when (it.isAutoTunnelPaused) {
                    true -> launchWatcherPausedNotification()
                    false -> launchWatcherNotification()
                }
            }
            networkEventsFlow.value =
                networkEventsFlow.value.copy(
                    settings = it,
                )
        }
    }

    private suspend fun watchForVpnConnectivityChanges() {
        vpnService.vpnState.collect {
            networkEventsFlow.value =
                networkEventsFlow.value.copy(
                    vpnStatus = it.status,
                    config = it.tunnelConfig,
                )
        }
    }

    private suspend fun watchForEthernetConnectivityChanges() {
        ethernetService.networkStatus.collect {
            when (it) {
                is NetworkStatus.Available -> {
                    Timber.i("Gained Ethernet connection")
                    networkEventsFlow.value =
                        networkEventsFlow.value.copy(
                            isEthernetConnected = true,
                        )
                }

                is NetworkStatus.CapabilitiesChanged -> {
                    Timber.i("Ethernet capabilities changed")
                    networkEventsFlow.value =
                        networkEventsFlow.value.copy(
                            isEthernetConnected = true,
                        )
                }

                is NetworkStatus.Unavailable -> {
                    networkEventsFlow.value =
                        networkEventsFlow.value.copy(
                            isEthernetConnected = false,
                        )
                    Timber.i("Lost Ethernet connection")
                }
            }
        }
    }

    private suspend fun watchForWifiConnectivityChanges() {
        wifiService.networkStatus.collect {
            when (it) {
                is NetworkStatus.Available -> {
                    Timber.i("Gained Wi-Fi connection")
                    networkEventsFlow.value =
                        networkEventsFlow.value.copy(
                            isWifiConnected = true,
                        )
                }

                is NetworkStatus.CapabilitiesChanged -> {
                    Timber.i("Wifi capabilities changed")
                    networkEventsFlow.value =
                        networkEventsFlow.value.copy(
                            isWifiConnected = true,
                        )
                    val ssid = wifiService.getNetworkName(it.networkCapabilities)
                    ssid?.let {
                        if(it.contains(Constants.UNREADABLE_SSID)) {
                            Timber.w("SSID unreadable: missing permissions")
                        } else Timber.i("Detected valid SSID")
                        appDataRepository.appState.setCurrentSsid(ssid)
                        networkEventsFlow.value =
                            networkEventsFlow.value.copy(
                                currentNetworkSSID = ssid,
                            )
                    } ?: Timber.w("Failed to read ssid")
                }

                is NetworkStatus.Unavailable -> {
                    networkEventsFlow.value =
                        networkEventsFlow.value.copy(
                            isWifiConnected = false,
                        )
                    Timber.i("Lost Wi-Fi connection")
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

    private suspend fun manageVpn() {
        networkEventsFlow.collectLatest { watcherState ->
            val autoTunnel = "Auto-tunnel watcher"
            if (!watcherState.settings.isAutoTunnelPaused) {
                //delay for rapid network state changes and then collect latest
                delay(Constants.WATCHER_COLLECTION_DELAY)
                when {
                    watcherState.isEthernetConditionMet() -> {
                        Timber.i("$autoTunnel - tunnel on on ethernet condition met")
                        serviceManager.startVpnServiceForeground(this)
                    }

                    watcherState.isMobileDataConditionMet() -> {
                        Timber.i("$autoTunnel - tunnel on on mobile data condition met")
                        serviceManager.startVpnServiceForeground(this, getMobileDataTunnel()?.id)
                    }

                    watcherState.isTunnelNotMobileDataPreferredConditionMet() -> {
                        getMobileDataTunnel()?.let {
                            Timber.i("$autoTunnel - tunnel connected on mobile data is not preferred condition met, switching to preferred")
                            serviceManager.startVpnServiceForeground(
                                this,
                                getMobileDataTunnel()?.id,
                            )
                        }
                    }

                    watcherState.isTunnelOffOnMobileDataConditionMet() -> {
                        Timber.i("$autoTunnel - tunnel off on mobile data met, turning vpn off")
                        serviceManager.stopVpnServiceForeground(this)
                    }

                    watcherState.isTunnelNotWifiNamePreferredMet(watcherState.currentNetworkSSID) -> {
                        Timber.i("$autoTunnel - tunnel on ssid not associated with current tunnel condition met")
                        getSsidTunnel(watcherState.currentNetworkSSID)?.let {
                            Timber.i("Found tunnel associated with this SSID, bringing tunnel up")
                            serviceManager.startVpnServiceForeground(this, it.id)
                        } ?: suspend {
                            Timber.i("No tunnel associated with this SSID, using defaults")
                            if (appDataRepository.getPrimaryOrFirstTunnel()?.name != vpnService.name) {
                                serviceManager.startVpnServiceForeground(this)
                            }
                        }.invoke()
                    }

                    watcherState.isUntrustedWifiConditionMet() -> {
                        Timber.i("$autoTunnel - tunnel on untrusted wifi condition met")
                        serviceManager.startVpnServiceForeground(
                            this,
                            getSsidTunnel(watcherState.currentNetworkSSID)?.id,
                        )
                    }

                    watcherState.isTrustedWifiConditionMet() -> {
                        Timber.i("$autoTunnel - tunnel off on trusted wifi condition met, turning vpn off")
                        serviceManager.stopVpnServiceForeground(this)
                    }

                    watcherState.isTunnelOffOnWifiConditionMet() -> {
                        Timber.i("$autoTunnel - tunnel off on wifi condition met, turning vpn off")
                        serviceManager.stopVpnServiceForeground(this)
                    }

                    watcherState.isTunnelOffOnNoConnectivityMet() -> {
                        Timber.i("$autoTunnel - tunnel off on no connectivity met, turning vpn off")
                        serviceManager.stopVpnServiceForeground(this)
                    }

                    else -> {
                        Timber.i("$autoTunnel - no condition met")
                    }
                }
            }
        }
    }
}
