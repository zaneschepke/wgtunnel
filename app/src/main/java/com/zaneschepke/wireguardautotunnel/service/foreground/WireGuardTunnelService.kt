package com.zaneschepke.wireguardautotunnel.service.foreground

import android.os.Bundle
import com.wireguard.android.backend.Tunnel
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.service.notification.NotificationService
import com.zaneschepke.wireguardautotunnel.service.tunnel.VpnService
import com.zaneschepke.wireguardautotunnel.service.tunnel.model.TunnelConfig
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class WireGuardTunnelService : ForegroundService() {

    @Inject
    lateinit var vpnService : VpnService

    @Inject
    lateinit var notificationService : NotificationService

    private lateinit var job : Job

    @OptIn(DelicateCoroutinesApi::class)
    override fun startService(extras : Bundle?) {
        super.startService(extras)
        val tunnelConfigString = extras?.getString(getString(R.string.tunnel_extras_key))
        cancelJob()
        job = GlobalScope.launch {
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
            description = "${getString(R.string.tunnel_start_text)} $tunnelName"
        )
        super.startForeground(1, notification)
    }
    private fun cancelJob() {
        if(this::job.isInitialized) {
            job.cancel()
        }
    }
}