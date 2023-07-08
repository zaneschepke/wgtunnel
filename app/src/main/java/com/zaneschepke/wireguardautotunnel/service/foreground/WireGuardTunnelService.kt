package com.zaneschepke.wireguardautotunnel.service.foreground

import android.os.Bundle
import com.wireguard.android.backend.Tunnel
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.service.notification.NotificationService
import com.zaneschepke.wireguardautotunnel.service.tunnel.VpnService
import com.zaneschepke.wireguardautotunnel.service.tunnel.model.TunnelConfig
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class WireGuardTunnelService : ForegroundService() {

    private val foregroundId = 123;

    @Inject
    lateinit var vpnService : VpnService

    @Inject
    lateinit var notificationService : NotificationService

    private lateinit var job : Job

    override fun startService(extras : Bundle?) {
        super.startService(extras)
        val tunnelConfigString = extras?.getString(getString(R.string.tunnel_extras_key))
        cancelJob()
        job = CoroutineScope(SupervisorJob()).launch {
            if(tunnelConfigString != null) {
                try {
                    val tunnelConfig = TunnelConfig.from(tunnelConfigString)
                    val state = vpnService.startTunnel(tunnelConfig)
                    if (state == Tunnel.State.UP) {
                        launchVpnConnectedNotification(tunnelConfig.name)
                    }
                } catch (e : Exception) {
                    Timber.e("Problem starting tunnel: ${e.message}")
                    stopService(extras)
                }
            } else {
                Timber.e("Tunnel config null")
            }
        }
    }

    override fun stopService(extras : Bundle?) {
        super.stopService(extras)
        CoroutineScope(Dispatchers.IO).launch() {
            vpnService.stopTunnel()
        }
        cancelJob()
        stopSelf()
    }

    private fun launchVpnConnectedNotification(tunnelName : String) {
        val notification = notificationService.createNotification(
            channelId = getString(R.string.vpn_channel_id),
            channelName = getString(R.string.vpn_channel_name),
            title = getString(R.string.tunnel_start_title),
            onGoing = false,
            showTimestamp = true,
            description = "${getString(R.string.tunnel_start_text)} $tunnelName"
        )
        super.startForeground(foregroundId, notification)
    }
    private fun cancelJob() {
        if(this::job.isInitialized) {
            job.cancel()
        }
    }
}