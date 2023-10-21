package com.zaneschepke.wireguardautotunnel.service.shortcut

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.zaneschepke.wireguardautotunnel.repository.SettingsDoa
import com.zaneschepke.wireguardautotunnel.repository.TunnelConfigDao
import com.zaneschepke.wireguardautotunnel.repository.model.Settings
import com.zaneschepke.wireguardautotunnel.repository.model.TunnelConfig
import com.zaneschepke.wireguardautotunnel.service.foreground.Action
import com.zaneschepke.wireguardautotunnel.service.foreground.ServiceManager
import com.zaneschepke.wireguardautotunnel.service.foreground.WireGuardTunnelService
import com.zaneschepke.wireguardautotunnel.util.WgTunnelException
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class ShortcutsActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepo : SettingsDoa

    @Inject
    lateinit var tunnelConfigRepo : TunnelConfigDao

    private fun attemptWatcherServiceToggle(tunnelConfig : String) {
        lifecycleScope.launch(Dispatchers.Main) {
            val settings = getSettings()
            if(settings.isAutoTunnelEnabled) {
                ServiceManager.toggleWatcherServiceForeground(this@ShortcutsActivity, tunnelConfig)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if(intent.getStringExtra(CLASS_NAME_EXTRA_KEY)
            .equals(WireGuardTunnelService::class.java.simpleName)) {
            lifecycleScope.launch(Dispatchers.Main) {
                val settings = getSettings()
                if(settings.isShortcutsEnabled) {
                    try {
                        val tunnelName = intent.getStringExtra(TUNNEL_NAME_EXTRA_KEY)
                        val tunnelConfig = if(tunnelName != null) {
                            tunnelConfigRepo.getAll().firstOrNull { it.name == tunnelName }
                        } else {
                            if(settings.defaultTunnel == null) {
                                tunnelConfigRepo.getAll().first()
                            } else {
                                TunnelConfig.from(settings.defaultTunnel!!)
                            }
                        }
                        tunnelConfig ?: return@launch
                        attemptWatcherServiceToggle(tunnelConfig.toString())
                        when(intent.action){
                            Action.STOP.name -> ServiceManager.stopVpnService(this@ShortcutsActivity)
                            Action.START.name -> ServiceManager.startVpnService(this@ShortcutsActivity, tunnelConfig.toString())
                        }
                    } catch (e : Exception) {
                        Timber.e(e.message)
                    }
                }
            }
        }
        finish()
    }

    private suspend fun getSettings() : Settings {
        val settings = settingsRepo.getAll()
        return if (settings.isNotEmpty()) {
            settings.first()
        } else {
            throw WgTunnelException("Settings empty")
        }
    }
    companion object {
        const val TUNNEL_NAME_EXTRA_KEY = "tunnelName"
        const val CLASS_NAME_EXTRA_KEY = "className"
    }
}