package com.zaneschepke.wireguardautotunnel.service.foreground

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.PowerManager
import android.os.SystemClock
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
import kotlinx.coroutines.withContext
import timber.log.Timber
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
      val isVpnConnected : Boolean = false,
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
          if(settingsRepository.getSettings().isAutoTunnelPaused) {
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

  private fun launchWatcherNotification(description: String = getString(R.string.watcher_notification_text_active)) {
    val notification =
        notificationService.createNotification(
            channelId = getString(R.string.watcher_channel_id),
            channelName = getString(R.string.watcher_channel_name),
            title = getString(R.string.auto_tunnel_title),
            description = description)
    ServiceCompat.startForeground(
        this, foregroundId, notification, Constants.SYSTEM_EXEMPT_SERVICE_TYPE_ID)
  }

    private fun launchWatcherPausedNotification() {
        launchWatcherNotification(getString(R.string.watcher_notification_text_paused))
    }

  // TODO could this be restarting service in a bad state?
  // try to start task again if killed
  override fun onTaskRemoved(rootIntent: Intent) {
    Timber.d("Task Removed called")
    val restartServiceIntent = Intent(rootIntent)
    val restartServicePendingIntent: PendingIntent =
        PendingIntent.getService(
            this,
            1,
            restartServiceIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE)
    applicationContext.getSystemService(Context.ALARM_SERVICE)
    val alarmService: AlarmManager =
        applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    alarmService.set(
        AlarmManager.ELAPSED_REALTIME,
        SystemClock.elapsedRealtime() + 1000,
        restartServicePendingIntent)
  }

  private suspend fun initWakeLock() {
    val isBatterySaverOn =
        withContext(lifecycleScope.coroutineContext) {
          settingsRepository.getSettings().isBatterySaverEnabled
        }
    wakeLock =
        (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
          newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "$tag::lock").apply {
            if (isBatterySaverOn) {
              Timber.d("Initiating wakelock with timeout")
              acquire(Constants.BATTERY_SAVER_WATCHER_WAKE_LOCK_TIMEOUT)
            } else {
              Timber.d("Initiating wakelock with zero timeout")
              acquire(Constants.DEFAULT_WATCHER_WAKE_LOCK_TIMEOUT)
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
            Timber.d("Starting wifi watcher")
            watchForWifiConnectivityChanges()
          }
          if (setting.isTunnelOnMobileDataEnabled) {
            launch {
              Timber.d("Starting mobile data watcher")
              watchForMobileDataConnectivityChanges()
            }
          }
          if (setting.isTunnelOnEthernetEnabled) {
            launch {
              Timber.d("Starting ethernet data watcher")
              watchForEthernetConnectivityChanges()
            }
          }
            launch {
                Timber.d("Starting vpn state watcher")
                watchForVpnConnectivityChanges()
            }
            launch {
                Timber.d("Starting settings watcher")
                watchForSettingsChanges()
            }
          launch {
            Timber.d("Starting management watcher")
            manageVpn()
          }
        }
  }

  private suspend fun watchForMobileDataConnectivityChanges() {
    mobileDataService.networkStatus.collect {
      when (it) {
        is NetworkStatus.Available -> {
          Timber.d("Gained Mobile data connection")
            networkEventsFlow.value = networkEventsFlow.value.copy(
                isMobileDataConnected = true
            )
        }
        is NetworkStatus.CapabilitiesChanged -> {
            networkEventsFlow.value = networkEventsFlow.value.copy(
                isMobileDataConnected = true
            )
          Timber.d("Mobile data capabilities changed")
        }
        is NetworkStatus.Unavailable -> {
            networkEventsFlow.value = networkEventsFlow.value.copy(
                isMobileDataConnected = false
            )
          Timber.d("Lost mobile data connection")
        }
      }
    }
  }
    private suspend fun watchForSettingsChanges() {
        settingsRepository.getSettingsFlow().collect {
            if(networkEventsFlow.value.settings.isAutoTunnelPaused != it.isAutoTunnelPaused) {
                when(it.isAutoTunnelPaused) {
                    true -> launchWatcherPausedNotification()
                    false -> launchWatcherNotification()
                }
            }
            networkEventsFlow.value = networkEventsFlow.value.copy(
                settings = it
            )
        }
    }

    private suspend fun watchForVpnConnectivityChanges() {
        vpnService.vpnState.collect {
            when(it.status) {
                Tunnel.State.DOWN -> networkEventsFlow.value = networkEventsFlow.value.copy(
                    isVpnConnected = false
                )
                Tunnel.State.UP -> networkEventsFlow.value = networkEventsFlow.value.copy(
                    isVpnConnected = true
                )
                else -> {}
            }
        }
    }

  private suspend fun watchForEthernetConnectivityChanges() {
    ethernetService.networkStatus.collect {
      when (it) {
        is NetworkStatus.Available -> {
          Timber.d("Gained Ethernet connection")
            networkEventsFlow.value = networkEventsFlow.value.copy(
                isEthernetConnected = true
            )
        }
        is NetworkStatus.CapabilitiesChanged -> {
          Timber.d("Ethernet capabilities changed")
            networkEventsFlow.value = networkEventsFlow.value.copy(
                isEthernetConnected = true
            )
        }
        is NetworkStatus.Unavailable -> {
            networkEventsFlow.value = networkEventsFlow.value.copy(
                isEthernetConnected = false
            )
          Timber.d("Lost Ethernet connection")
        }
      }
    }
  }

  private suspend fun watchForWifiConnectivityChanges() {
    wifiService.networkStatus.collect {
      when (it) {
        is NetworkStatus.Available -> {
          Timber.d("Gained Wi-Fi connection")
            networkEventsFlow.value = networkEventsFlow.value.copy(
                isWifiConnected = true
            )
        }
        is NetworkStatus.CapabilitiesChanged -> {
          Timber.d("Wifi capabilities changed")
            networkEventsFlow.value = networkEventsFlow.value.copy(
                isWifiConnected = true
            )
          val ssid = wifiService.getNetworkName(it.networkCapabilities) ?: ""
          Timber.d("Detected SSID: $ssid")
            networkEventsFlow.value = networkEventsFlow.value.copy(
                currentNetworkSSID = ssid
            )
        }
        is NetworkStatus.Unavailable -> {
          networkEventsFlow.value = networkEventsFlow.value.copy(
              isWifiConnected = false
          )
          Timber.d("Lost Wi-Fi connection")
        }
      }
    }
  }

    //TODO clean this up
  private suspend fun manageVpn() {
    networkEventsFlow.collectLatest {
        Timber.i("New watcher state: $it")
        if (!it.settings.isAutoTunnelPaused && it.settings.defaultTunnel != null) {
        delay(Constants.TOGGLE_TUNNEL_DELAY)
            when {
                ((it.isEthernetConnected &&
                        it.settings.isTunnelOnEthernetEnabled &&
                        !it.isVpnConnected)) -> {
                    ServiceManager.startVpnService(this, it.settings.defaultTunnel!!)
                    Timber.i("Condition 1 met")
                }
                (!it.isEthernetConnected &&
                        it.settings.isTunnelOnMobileDataEnabled &&
                        !it.isWifiConnected &&
                        it.isMobileDataConnected &&
                        !it.isVpnConnected) -> {
                    ServiceManager.startVpnService(this, it.settings.defaultTunnel!!)
                    Timber.i("Condition 2 met")
                }
                (!it.isEthernetConnected &&
                        !it.settings.isTunnelOnMobileDataEnabled &&
                        !it.isWifiConnected &&
                        it.isVpnConnected) -> {
                    ServiceManager.stopVpnService(this)
                    Timber.i("Condition 3 met")
                }
                (!it.isEthernetConnected &&
                        it.isWifiConnected &&
                        !it.settings.trustedNetworkSSIDs.contains(it.currentNetworkSSID) &&
                        it.settings.isTunnelOnWifiEnabled &&
                        (!it.isVpnConnected)) -> {
                    ServiceManager.startVpnService(this, it.settings.defaultTunnel!!)
                    Timber.i("Condition 4 met")
                }
                (!it.isEthernetConnected &&
                        (it.isWifiConnected && it.settings.trustedNetworkSSIDs.contains(it.currentNetworkSSID)) &&
                        (it.isVpnConnected)) -> {
                    ServiceManager.stopVpnService(this)
                    Timber.i("Condition 5 met")
                }
                (!it.isEthernetConnected &&
                        (it.isWifiConnected &&
                                !it.settings.isTunnelOnWifiEnabled &&
                                (it.isVpnConnected))) -> {
                    ServiceManager.stopVpnService(this)
                    Timber.i("Condition 6 met")
                }
                (!it.isEthernetConnected &&
                        !it.isWifiConnected &&
                        !it.isMobileDataConnected &&
                        (it.isVpnConnected)) -> {
                    ServiceManager.stopVpnService(this)
                    Timber.i("Condition 7 met")
                }
                else -> {
                    Timber.i("No condition met")
                }
            }
        }
    }
  }
}
