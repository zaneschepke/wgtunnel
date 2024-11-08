package com.zaneschepke.wireguardautotunnel.service.foreground

import android.app.Service
import android.content.Context
import android.content.Intent
import com.zaneschepke.wireguardautotunnel.util.SingletonHolder
import com.zaneschepke.wireguardautotunnel.util.extensions.requestTunnelTileServiceStateUpdate
import jakarta.inject.Inject
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import timber.log.Timber

@OptIn(ExperimentalCoroutinesApi::class)
class ServiceManager
@Inject constructor(private val context: Context) {

	private val _autoTunnelActive = MutableStateFlow(false)

	val autoTunnelActive = _autoTunnelActive.asStateFlow()

	var autoTunnelService = CompletableDeferred<AutoTunnelService>()
	var backgroundService = CompletableDeferred<TunnelBackgroundService>()

	companion object : SingletonHolder<ServiceManager, Context>(::ServiceManager)

	private fun <T : Service> startService(cls: Class<T>, background: Boolean) {
		runCatching {
			val intent = Intent(context, cls)
			if (background) {
				context.startForegroundService(intent)
			} else {
				context.startService(intent)
			}
		}.onFailure { Timber.e(it) }
	}

	suspend fun startAutoTunnel(background: Boolean) {
		if (autoTunnelService.isCompleted) return _autoTunnelActive.update { true }
		kotlin.runCatching {
			startService(AutoTunnelService::class.java, background)
			autoTunnelService.await()
			autoTunnelService.getCompleted().start()
			_autoTunnelActive.update { true }
		}.onFailure {
			Timber.e(it)
		}
	}

	suspend fun startBackgroundService() {
		if (backgroundService.isCompleted) return
		kotlin.runCatching {
			startService(TunnelBackgroundService::class.java, true)
			backgroundService.await()
			backgroundService.getCompleted().start()
		}.onFailure {
			Timber.e(it)
		}
	}

	fun stopBackgroundService() {
		if (!backgroundService.isCompleted) return
		runCatching {
			backgroundService.getCompleted().stop()
		}.onFailure {
			Timber.e(it)
		}
	}

	fun stopAutoTunnel() {
		if (!autoTunnelService.isCompleted) return
		runCatching {
			autoTunnelService.getCompleted().stop()
			_autoTunnelActive.update { false }
		}.onFailure {
			Timber.e(it)
		}
	}

	fun requestTunnelTileUpdate() {
		context.requestTunnelTileServiceStateUpdate()
	}
}
