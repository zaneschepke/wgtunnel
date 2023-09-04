package com.zaneschepke.wireguardautotunnel.service.shortcut

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.service.foreground.Action
import com.zaneschepke.wireguardautotunnel.service.foreground.ServiceManager
import com.zaneschepke.wireguardautotunnel.service.foreground.WireGuardTunnelService
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ShortcutsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if(intent.getStringExtra(ShortcutsManager.CLASS_NAME_EXTRA_KEY)
            .equals(WireGuardTunnelService::class.java.name)) {
            intent.getStringExtra(getString(R.string.tunnel_extras_key))?.let {
                ServiceManager.toggleWatcherService(this, it)
            }
            when(intent.action){
                Action.STOP.name -> ServiceManager.stopVpnService(this)
                Action.START.name -> intent.getStringExtra(getString(R.string.tunnel_extras_key))
                    ?.let { ServiceManager.startVpnService(this, it) }
            }
        }
        finish()
    }
}