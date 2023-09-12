package com.zaneschepke.wireguardautotunnel.service.shortcut

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.repository.Repository
import com.zaneschepke.wireguardautotunnel.service.foreground.Action
import com.zaneschepke.wireguardautotunnel.service.foreground.ServiceManager
import com.zaneschepke.wireguardautotunnel.service.foreground.WireGuardTunnelService
import com.zaneschepke.wireguardautotunnel.service.tunnel.model.Settings
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ShortcutsActivity : AppCompatActivity() {

    @Inject
    lateinit var settingsRepo : Repository<Settings>

    private val scope = CoroutineScope(Dispatchers.Main);

    private fun attemptWatcherServiceToggle(tunnelConfig : String) {
        scope.launch {
            val settings = settingsRepo.getAll()
            if (!settings.isNullOrEmpty()) {
                val setting = settings.first()
                if(setting.isAutoTunnelEnabled) {
                    ServiceManager.toggleWatcherServiceForeground(this@ShortcutsActivity, tunnelConfig)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if(intent.getStringExtra(ShortcutsManager.CLASS_NAME_EXTRA_KEY)
            .equals(WireGuardTunnelService::class.java.name)) {

            intent.getStringExtra(getString(R.string.tunnel_extras_key))?.let {
                attemptWatcherServiceToggle(it)
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