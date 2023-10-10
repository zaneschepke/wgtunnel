package com.zaneschepke.wireguardautotunnel

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import com.zaneschepke.wireguardautotunnel.repository.SettingsDoa
import com.zaneschepke.wireguardautotunnel.repository.model.Settings
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class WireGuardAutoTunnel : Application() {

    @Inject
    lateinit var settingsRepo : SettingsDoa

    override fun onCreate() {
        super.onCreate()
        if(BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        initSettings()
    }

    private fun initSettings() {
        CoroutineScope(Dispatchers.IO).launch {
            if(settingsRepo.getAll().isEmpty()) {
                settingsRepo.save(Settings())
            }
        }
    }

    companion object {
        fun isRunningOnAndroidTv(context : Context) : Boolean {
            return context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
        }
    }
}