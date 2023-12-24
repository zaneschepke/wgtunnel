package com.zaneschepke.wireguardautotunnel

import android.app.Application
import android.content.ComponentName
import android.content.pm.PackageManager
import android.service.quicksettings.TileService
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.zaneschepke.wireguardautotunnel.data.datastore.DataStoreManager
import com.zaneschepke.wireguardautotunnel.data.model.Settings
import com.zaneschepke.wireguardautotunnel.data.repository.SettingsRepository
import com.zaneschepke.wireguardautotunnel.service.tile.TunnelControlTile
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject

@HiltAndroidApp
class WireGuardAutoTunnel : Application() {
    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var dataStoreManager: DataStoreManager

    override fun onCreate() {
        super.onCreate()
        instance = this
        if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())
        initSettings()
        with(ProcessLifecycleOwner.get()) {
            lifecycleScope.launch {
                try {
                    // load preferences into memory
                    dataStoreManager.init()
                    requestTileServiceStateUpdate()
                } catch (e: IOException) {
                    Timber.e("Failed to load preferences")
                }
            }
        }
    }

    private fun initSettings() {
        with(ProcessLifecycleOwner.get()) {
            lifecycleScope.launch {
                if (settingsRepository.getAll().isEmpty()) {
                    settingsRepository.save(Settings())
                }
            }
        }
    }

    companion object {
        lateinit var instance: WireGuardAutoTunnel private set
        fun isRunningOnAndroidTv(): Boolean {
            return instance.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
        }
        fun requestTileServiceStateUpdate() {
            TileService.requestListeningState(instance, ComponentName(instance, TunnelControlTile::class.java))
        }
    }
}
