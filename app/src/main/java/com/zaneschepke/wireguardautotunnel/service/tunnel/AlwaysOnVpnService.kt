package com.zaneschepke.wireguardautotunnel.service.tunnel

import android.content.Intent
import android.os.IBinder
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.zaneschepke.wireguardautotunnel.data.repository.AppDataRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Provider

@AndroidEntryPoint
class AlwaysOnVpnService : LifecycleService() {

	@Inject
	lateinit var appDataRepository: AppDataRepository

	override fun onBind(intent: Intent): IBinder? {
		super.onBind(intent)
		// We don't provide binding, so return null
		return null
	}

	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
		if (intent == null || intent.component == null || intent.component!!.packageName != packageName) {
			Timber.i("Always-on VPN requested started")
			lifecycleScope.launch {
				val settings = appDataRepository.settings.getSettings()
				if (settings.isAlwaysOnVpnEnabled) {
					val tunnel = appDataRepository.getPrimaryOrFirstTunnel()
					tunnel?.let {
//						tunnelService.get().startTunnel(it)
					}
				} else {
					Timber.w("Always-on VPN is not enabled in app settings")
				}
			}
		}
		return super.onStartCommand(intent, flags, startId)
	}
}
