package com.zaneschepke.wireguardautotunnel

import android.app.Application
import com.zaneschepke.wireguardautotunnel.repository.Repository
import com.zaneschepke.wireguardautotunnel.service.tunnel.model.Settings
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class WireGuardAutoTunnel : Application() {

    @Inject
    lateinit var settingsRepo : Repository<Settings>

    override fun onCreate() {
        super.onCreate()
        if(BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())
        settingsRepo.init()
    }
}