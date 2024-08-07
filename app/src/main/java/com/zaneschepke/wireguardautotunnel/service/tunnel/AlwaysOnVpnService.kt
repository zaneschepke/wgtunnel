package com.zaneschepke.wireguardautotunnel.service.tunnel

import android.content.Intent
import android.os.IBinder
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.zaneschepke.wireguardautotunnel.data.repository.AppDataRepository
import com.zaneschepke.wireguardautotunnel.module.IoDispatcher
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class AlwaysOnVpnService : LifecycleService() {

	@Inject
	lateinit var tunnelService: TunnelService

	@Inject
	lateinit var appDataRepository: AppDataRepository

	@Inject
	@IoDispatcher
	lateinit var ioDispatcher: CoroutineDispatcher

	override fun onBind(intent: Intent): IBinder? {
		super.onBind(intent)
		// We don't provide binding, so return null
		return null
	}

	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
		if (intent == null || intent.component == null || intent.component!!.packageName != packageName) {
			Timber.i("Always-on VPN requested started")
			lifecycleScope.launch(ioDispatcher) {
				val settings = appDataRepository.settings.getSettings()
				if (settings.isAlwaysOnVpnEnabled) {
					val tunnel = appDataRepository.getPrimaryOrFirstTunnel()
					tunnel?.let {
						tunnelService.startTunnel(it)
					}
				} else {
					Timber.w("Always-on VPN is not enabled in app settings")
				}
			}
		}
		return super.onStartCommand(intent, flags, startId)
	}
}
