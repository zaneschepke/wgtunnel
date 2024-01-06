package com.zaneschepke.wireguardautotunnel.service.shortcut

import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.zaneschepke.wireguardautotunnel.data.model.TunnelConfig
import com.zaneschepke.wireguardautotunnel.data.repository.SettingsRepository
import com.zaneschepke.wireguardautotunnel.data.repository.TunnelConfigRepository
import com.zaneschepke.wireguardautotunnel.service.foreground.Action
import com.zaneschepke.wireguardautotunnel.service.foreground.ServiceManager
import com.zaneschepke.wireguardautotunnel.service.foreground.WireGuardTunnelService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class ShortcutsActivity : ComponentActivity() {
    @Inject lateinit var settingsRepository: SettingsRepository

    @Inject lateinit var tunnelConfigRepository: TunnelConfigRepository

    private suspend fun toggleWatcherServicePause() {
        val settings = settingsRepository.getSettings()
        if (settings.isAutoTunnelEnabled) {
            val pauseAutoTunnel = !settings.isAutoTunnelPaused
            settingsRepository.save(
                settings.copy(
                    isAutoTunnelPaused = pauseAutoTunnel,
                ),
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(View(this))
        if (
            intent
                .getStringExtra(CLASS_NAME_EXTRA_KEY)
                .equals(WireGuardTunnelService::class.java.simpleName)
        ) {
            lifecycleScope.launch(Dispatchers.Main) {
                val settings = settingsRepository.getSettings()
                if (settings.isShortcutsEnabled) {
                    try {
                        val tunnelName = intent.getStringExtra(TUNNEL_NAME_EXTRA_KEY)
                        val tunnelConfig =
                            if (tunnelName != null) {
                                tunnelConfigRepository.getAll().firstOrNull {
                                    it.name == tunnelName
                                }
                            } else {
                                if (settings.defaultTunnel == null) {
                                    tunnelConfigRepository.getAll().first()
                                } else {
                                    TunnelConfig.from(settings.defaultTunnel!!)
                                }
                            }
                        tunnelConfig ?: return@launch
                        toggleWatcherServicePause()
                        when (intent.action) {
                            Action.STOP.name ->
                                ServiceManager.stopVpnService(
                                    this@ShortcutsActivity,
                                )
                            Action.START.name ->
                                ServiceManager.startVpnServiceForeground(
                                    this@ShortcutsActivity,
                                    tunnelConfig.toString(),
                                )
                        }
                    } catch (e: Exception) {
                        Timber.e(e.message)
                        finish()
                    }
                }
            }
        }
        finish()
    }

    companion object {
        const val TUNNEL_NAME_EXTRA_KEY = "tunnelName"
        const val CLASS_NAME_EXTRA_KEY = "className"
    }
}
