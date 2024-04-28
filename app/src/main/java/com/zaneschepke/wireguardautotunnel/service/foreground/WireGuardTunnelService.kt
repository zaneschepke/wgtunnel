package com.zaneschepke.wireguardautotunnel.service.foreground

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import androidx.core.app.ServiceCompat
import androidx.lifecycle.lifecycleScope
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.data.repository.AppDataRepository
import com.zaneschepke.wireguardautotunnel.receiver.NotificationActionReceiver
import com.zaneschepke.wireguardautotunnel.service.notification.NotificationService
import com.zaneschepke.wireguardautotunnel.service.tunnel.HandshakeStatus
import com.zaneschepke.wireguardautotunnel.service.tunnel.VpnService
import com.zaneschepke.wireguardautotunnel.util.Constants
import com.zaneschepke.wireguardautotunnel.util.handshakeStatus
import com.zaneschepke.wireguardautotunnel.util.mapPeerStats
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.amnezia.awg.backend.Tunnel
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class WireGuardTunnelService : ForegroundService() {
    private val foregroundId = 123

    @Inject
    lateinit var vpnService: VpnService

    @Inject
    lateinit var appDataRepository: AppDataRepository

    @Inject
    lateinit var notificationService: NotificationService

    private var job: Job? = null

    private var didShowConnected = false

    override fun onCreate() {
        super.onCreate()
        lifecycleScope.launch(Dispatchers.Main) {
            //TODO fix this to not launch if AOVPN
            if (appDataRepository.tunnels.count() != 0) {
                launchVpnNotification()
            }
        }
    }

    override fun startService(extras: Bundle?) {
        super.startService(extras)
        cancelJob()
        job =
            lifecycleScope.launch(Dispatchers.IO) {
                launch {
                    val tunnelId = extras?.getInt(Constants.TUNNEL_EXTRA_KEY)
                    if (vpnService.getState() == Tunnel.State.UP) {
                        vpnService.stopTunnel()
                    }
                    vpnService.startTunnel(
                        tunnelId?.let {
                            appDataRepository.tunnels.getById(it)
                        },
                    )
                }
                launch {
                    handshakeNotifications()
                }
            }
    }

    //TODO improve tunnel notifications
    private suspend fun handshakeNotifications() {
        var tunnelName: String? = null
        vpnService.vpnState.collect { state ->
            state.statistics
                ?.mapPeerStats()
                ?.map { it.value?.handshakeStatus() }
                .let { statuses ->
                    when {
                        statuses?.all { it == HandshakeStatus.HEALTHY } == true -> {
                            if (!didShowConnected) {
                                delay(Constants.VPN_CONNECTED_NOTIFICATION_DELAY)
                                tunnelName = state.tunnelConfig?.name
                                launchVpnNotification(
                                    getString(R.string.tunnel_start_title),
                                    "${getString(R.string.tunnel_start_text)} - $tunnelName",
                                )
                                didShowConnected = true
                            }
                        }

                        statuses?.any { it == HandshakeStatus.STALE } == true -> {}
                        statuses?.all { it == HandshakeStatus.NOT_STARTED } ==
                            true -> {
                        }

                        else -> {}
                    }
                }
            if (state.status == Tunnel.State.UP && state.tunnelConfig?.name != tunnelName) {
                tunnelName = state.tunnelConfig?.name
                launchVpnNotification(
                    getString(R.string.tunnel_start_title),
                    "${getString(R.string.tunnel_start_text)} - $tunnelName",
                )
            }
        }
    }

    private fun launchAlwaysOnDisabledNotification() {
        launchVpnNotification(
            title = this.getString(R.string.vpn_connection_failed),
            description = this.getString(R.string.always_on_disabled),
        )
    }

    override fun stopService() {
        super.stopService()
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
        try {
            job?.cancel()
        } catch (e : CancellationException) {
            Timber.i("Tunnel job cancelled")
        }
    }
}
