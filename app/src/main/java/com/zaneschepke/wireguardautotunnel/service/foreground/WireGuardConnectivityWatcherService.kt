package com.zaneschepke.wireguardautotunnel.service.foreground

import android.content.Context
import android.os.Bundle
import android.os.PowerManager
import androidx.core.app.ServiceCompat
import androidx.lifecycle.lifecycleScope
import com.wireguard.android.backend.Tunnel
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.data.model.Settings
import com.zaneschepke.wireguardautotunnel.data.repository.SettingsRepository
import com.zaneschepke.wireguardautotunnel.service.network.EthernetService
import com.zaneschepke.wireguardautotunnel.service.network.MobileDataService
import com.zaneschepke.wireguardautotunnel.service.network.NetworkService
import com.zaneschepke.wireguardautotunnel.service.network.NetworkStatus
import com.zaneschepke.wireguardautotunnel.service.network.WifiService
import com.zaneschepke.wireguardautotunnel.service.notification.NotificationService
import com.zaneschepke.wireguardautotunnel.service.tunnel.VpnService
import com.zaneschepke.wireguardautotunnel.util.Constants
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import java.net.InetAddress
import javax.inject.Inject


@AndroidEntryPoint
class WireGuardConnectivityWatcherService : ForegroundService() {
    private val foregroundId = 122

    @Inject lateinit var wifiService: NetworkService<WifiService>

    @Inject lateinit var mobileDataService: NetworkService<MobileDataService>

    @Inject lateinit var ethernetService: NetworkService<EthernetService>

    @Inject lateinit var settingsRepository: SettingsRepository

    @Inject lateinit var notificationService: NotificationService

    @Inject lateinit var vpnService: VpnService

    private val networkEventsFlow = MutableStateFlow(WatcherState())

    data class WatcherState(
        val isWifiConnected: Boolean = false,
        val isVpnConnected: Boolean = false,
        val isEthernetConnected: Boolean = false,
        val isMobileDataConnected: Boolean = false,
        val currentNetworkSSID: String = "",
        val settings: Settings = Settings()
    )

    private lateinit var watcherJob: Job

    private var wakeLock: PowerManager.WakeLock? = null
    private val tag = this.javaClass.name

    override fun onCreate() {
        super.onCreate()
        lifecycleScope.launch(Dispatchers.Main) {
            try {
                if (settingsRepository.getSettings().isAutoTunnelPaused) {
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

    override fun stopService(extras: Bundle?) {
        super.stopService(extras)
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
        if (this::watcherJob.isInitialized) {
            watcherJob.cancel()
        }
    }

    private fun startWatcherJob() {
        watcherJob =
            lifecycleScope.launch(Dispatchers.IO) {
                val setting = settingsRepository.getSettings()
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
                if(setting.isPingEnabled) {
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
                if(vpnService.vpnState.value.status == Tunnel.State.UP) {
                    val config = vpnService.vpnState.value.config
                    config?.let {
                        val results = it.peers.map { peer ->
                            val host = if(peer.endpoint.isPresent &&
                                peer.endpoint.get().resolved.isPresent)
                                peer.endpoint.get().resolved.get().host
                            else Constants.BACKUP_PING_HOST
                            Timber.i("Checking reachability of: $host")
                            val reachable = InetAddress.getByName(host).isReachable(Constants.PING_TIMEOUT.toInt())
                            Timber.i("Result: reachable - $reachable")
                            reachable
                        }
                        if(results.contains(false)) {
                            Timber.i("Restarting VPN for ping failure")
                            ServiceManager.stopVpnService(this)
                            delay(Constants.VPN_RESTART_DELAY)
                            val tunnel = networkEventsFlow.value.settings.defaultTunnel
                            ServiceManager.startVpnServiceForeground(this, tunnel!!)
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
        settingsRepository.getSettingsFlow().collect {
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
            when (it.status) {
                Tunnel.State.DOWN ->
                    networkEventsFlow.value =
                        networkEventsFlow.value.copy(
                            isVpnConnected = false,
                        )
                Tunnel.State.UP ->
                    networkEventsFlow.value =
                        networkEventsFlow.value.copy(
                            isVpnConnected = true,
                        )
                else -> {}
            }
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
                    val ssid = wifiService.getNetworkName(it.networkCapabilities) ?: ""
                    Timber.i("Detected SSID: $ssid")
                    networkEventsFlow.value =
                        networkEventsFlow.value.copy(
                            currentNetworkSSID = ssid,
                        )
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

    private suspend fun manageVpn() {
        networkEventsFlow.collectLatest {
            val autoTunnel = "Auto-tunnel watcher"
            if (!it.settings.isAutoTunnelPaused && it.settings.defaultTunnel != null) {
                delay(Constants.TOGGLE_TUNNEL_DELAY)
                when {
                    ((it.isEthernetConnected &&
                        it.settings.isTunnelOnEthernetEnabled &&
                        !it.isVpnConnected)) -> {
                        ServiceManager.startVpnServiceForeground(this, it.settings.defaultTunnel!!)
                        Timber.i("$autoTunnel condition 1 met")
                    }
                    (!it.isEthernetConnected &&
                        it.settings.isTunnelOnMobileDataEnabled &&
                        !it.isWifiConnected &&
                        it.isMobileDataConnected &&
                        !it.isVpnConnected) -> {
                        ServiceManager.startVpnServiceForeground(this, it.settings.defaultTunnel!!)
                        Timber.i("$autoTunnel condition 2 met")
                    }
                    (!it.isEthernetConnected &&
                        !it.settings.isTunnelOnMobileDataEnabled &&
                        !it.isWifiConnected &&
                        it.isVpnConnected) -> {
                        ServiceManager.stopVpnService(this)
                        Timber.i("$autoTunnel condition 3 met")
                    }
                    (!it.isEthernetConnected &&
                        it.isWifiConnected &&
                        !it.settings.trustedNetworkSSIDs.contains(it.currentNetworkSSID) &&
                        it.settings.isTunnelOnWifiEnabled &&
                        (!it.isVpnConnected)) -> {
                        ServiceManager.startVpnServiceForeground(this, it.settings.defaultTunnel!!)
                        Timber.i("$autoTunnel condition 4 met")
                    }
                    (!it.isEthernetConnected &&
                        (it.isWifiConnected &&
                            it.settings.trustedNetworkSSIDs.contains(it.currentNetworkSSID)) &&
                        (it.isVpnConnected)) -> {
                        ServiceManager.stopVpnService(this)
                        Timber.i("$autoTunnel condition 5 met")
                    }
                    (!it.isEthernetConnected &&
                        (it.isWifiConnected &&
                            !it.settings.isTunnelOnWifiEnabled &&
                            (it.isVpnConnected))) -> {
                        ServiceManager.stopVpnService(this)
                        Timber.i("$autoTunnel condition 6 met")
                    }
                    (!it.isEthernetConnected &&
                        !it.isWifiConnected &&
                        !it.isMobileDataConnected &&
                        (it.isVpnConnected)) -> {
                        ServiceManager.stopVpnService(this)
                        Timber.i("$autoTunnel condition 7 met")
                    }
                    else -> {
                        Timber.i("$autoTunnel no condition met")
                    }
                }
            }
        }
    }
}
