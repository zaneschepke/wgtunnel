package com.zaneschepke.wireguardautotunnel.service.foreground

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import androidx.core.app.ServiceCompat
import androidx.lifecycle.lifecycleScope
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.data.model.TunnelConfig
import com.zaneschepke.wireguardautotunnel.data.repository.SettingsRepository
import com.zaneschepke.wireguardautotunnel.data.repository.TunnelConfigRepository
import com.zaneschepke.wireguardautotunnel.receiver.NotificationActionReceiver
import com.zaneschepke.wireguardautotunnel.service.notification.NotificationService
import com.zaneschepke.wireguardautotunnel.service.tunnel.HandshakeStatus
import com.zaneschepke.wireguardautotunnel.service.tunnel.VpnService
import com.zaneschepke.wireguardautotunnel.util.Constants
import com.zaneschepke.wireguardautotunnel.util.handshakeStatus
import com.zaneschepke.wireguardautotunnel.util.mapPeerStats
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class WireGuardTunnelService : ForegroundService() {
    private val foregroundId = 123

    @Inject lateinit var vpnService: VpnService

    @Inject lateinit var settingsRepository: SettingsRepository

    @Inject lateinit var tunnelConfigRepository: TunnelConfigRepository

    @Inject lateinit var notificationService: NotificationService

    private lateinit var job: Job

    private var tunnelName: String = ""
    private var didShowConnected = false

    override fun onCreate() {
        super.onCreate()
        lifecycleScope.launch(Dispatchers.Main) {
            if (tunnelConfigRepository.getAll().isNotEmpty()) {
                launchVpnNotification()
            }
        }
    }

    override fun startService(extras: Bundle?) {
        super.startService(extras)
        cancelJob()
        val tunnelConfigString = extras?.getString(getString(R.string.tunnel_extras_key))
        val tunnelConfig = tunnelConfigString?.let { TunnelConfig.from(it) }
        tunnelName = tunnelConfig?.name ?: ""
        job =
            lifecycleScope.launch(Dispatchers.IO) {
                launch {
                    if (tunnelConfig != null) {
                        try {
                            tunnelName = tunnelConfig.name
                            vpnService.startTunnel(tunnelConfig)
                        } catch (e: Exception) {
                            Timber.e("Problem starting tunnel: ${e.message}")
                            stopService(extras)
                        }
                    } else {
                        Timber.d("Tunnel config null, starting default tunnel or first")
                        val settings = settingsRepository.getSettings()
                        val tunnels = tunnelConfigRepository.getAll()
                        if (settings.isAlwaysOnVpnEnabled) {
                            val tunnel =
                                if (settings.defaultTunnel != null) {
                                    TunnelConfig.from(settings.defaultTunnel!!)
                                } else if (tunnels.isNotEmpty()) {
                                    tunnels.first()
                                } else {
                                    null
                                }
                            if (tunnel != null) {
                                tunnelName = tunnel.name
                                vpnService.startTunnel(tunnel)
                            }
                        }
                    }
                }
                // TODO add failed to connect notification
                launch {
                    vpnService.vpnState.collect { state ->
                        state.statistics
                            ?.mapPeerStats()
                            ?.map { it.value?.handshakeStatus() }
                            .let { statuses ->
                                when {
                                    statuses?.all { it == HandshakeStatus.HEALTHY } == true -> {
                                        if (!didShowConnected) {
                                            delay(Constants.VPN_CONNECTED_NOTIFICATION_DELAY)
                                            launchVpnNotification(
                                                getString(R.string.tunnel_start_title),
                                                "${getString(R.string.tunnel_start_text)} $tunnelName",
                                            )
                                            didShowConnected = true
                                        }
                                    }
                                    statuses?.any { it == HandshakeStatus.STALE } == true -> {}
                                    statuses?.all { it == HandshakeStatus.NOT_STARTED } ==
                                        true -> {}
                                    else -> {}
                                }
                            }
                    }
                }
            }
    }

    override fun stopService(extras: Bundle?) {
        super.stopService(extras)
        lifecycleScope.launch(Dispatchers.IO) {
            vpnService.stopTunnel()
            didShowConnected = false
        }
        cancelJob()
        stopSelf()
    }

    private fun launchVpnNotification(
        title: String = getString(R.string.vpn_starting),
        description: String = getString(R.string.attempt_connection)
    ) {
        val notification =
            notificationService.createNotification(
                channelId = getString(R.string.vpn_channel_id),
                channelName = getString(R.string.vpn_channel_name),
                title = title,
                onGoing = false,
                vibration = false,
                showTimestamp = true,
                description = description,
            )
        ServiceCompat.startForeground(
            this,
            foregroundId,
            notification,
            Constants.SYSTEM_EXEMPT_SERVICE_TYPE_ID,
        )
    }

    private fun launchVpnConnectionFailedNotification(message: String) {
        val notification =
            notificationService.createNotification(
                channelId = getString(R.string.vpn_channel_id),
                channelName = getString(R.string.vpn_channel_name),
                action =
                    PendingIntent.getBroadcast(
                        this,
                        0,
                        Intent(this, NotificationActionReceiver::class.java),
                        PendingIntent.FLAG_IMMUTABLE,
                    ),
                actionText = getString(R.string.restart),
                title = getString(R.string.vpn_connection_failed),
                onGoing = false,
                vibration = true,
                showTimestamp = true,
                description = message,
            )
        ServiceCompat.startForeground(
            this,
            foregroundId,
            notification,
            Constants.SYSTEM_EXEMPT_SERVICE_TYPE_ID,
        )
    }

    private fun cancelJob() {
        if (this::job.isInitialized) {
            job.cancel()
        }
    }
}
