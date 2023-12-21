package com.zaneschepke.wireguardautotunnel

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.zaneschepke.wireguardautotunnel.repository.SettingsDoa
import com.zaneschepke.wireguardautotunnel.repository.datastore.DataStoreManager
import com.zaneschepke.wireguardautotunnel.repository.model.Settings
import dagger.hilt.android.HiltAndroidApp
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.launch
import timber.log.Timber

@HiltAndroidApp
class WireGuardAutoTunnel : Application() {
    @Inject
    lateinit var settingsRepo: SettingsDoa

    @Inject
    lateinit var dataStoreManager: DataStoreManager

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())
        initSettings()
        with(ProcessLifecycleOwner.get()) {
            lifecycleScope.launch {
                try {
                    // load preferences into memory
                    dataStoreManager.init()
                } catch (e: IOException) {
                    Timber.e("Failed to load preferences")
                }
            }
        }
    }

    private fun initSettings() {
        with(ProcessLifecycleOwner.get()) {
            lifecycleScope.launch {
                if (settingsRepo.getAll().isEmpty()) {
                    settingsRepo.save(Settings())
                }
            }
        }
    }

    companion object {
        fun isRunningOnAndroidTv(context: Context): Boolean {
            return context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
        }
    }
}
