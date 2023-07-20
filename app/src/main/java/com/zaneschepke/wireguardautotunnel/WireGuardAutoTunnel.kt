package com.zaneschepke.wireguardautotunnel

import android.app.Application
import com.google.firebase.crashlytics.FirebaseCrashlytics
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
        if(BuildConfig.DEBUG) {
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(false);
            Timber.plant(Timber.DebugTree())
        }
        settingsRepo.init()
    }
}