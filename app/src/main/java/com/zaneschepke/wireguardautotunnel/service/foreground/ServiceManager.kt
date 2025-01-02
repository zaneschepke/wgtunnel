package com.zaneschepke.wireguardautotunnel.service.foreground

import android.app.Service
import android.content.Context
import android.content.Intent
import com.zaneschepke.wireguardautotunnel.data.domain.TunnelConfig
import com.zaneschepke.wireguardautotunnel.data.repository.AppDataRepository
import com.zaneschepke.wireguardautotunnel.service.foreground.autotunnel.AutoTunnelService
import com.zaneschepke.wireguardautotunnel.service.tile.AutoTunnelControlTile
import com.zaneschepke.wireguardautotunnel.service.tile.TunnelControlTile
import com.zaneschepke.wireguardautotunnel.util.extensions.requestAutoTunnelTileServiceUpdate
import com.zaneschepke.wireguardautotunnel.util.extensions.requestTunnelTileServiceStateUpdate
import jakarta.inject.Inject
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import timber.log.Timber

@OptIn(ExperimentalCoroutinesApi::class)
class ServiceManager
@Inject constructor(private val context: Context, private val ioDispatcher: CoroutineDispatcher, private val appDataRepository: AppDataRepository) {

	private val _autoTunnelActive = MutableStateFlow(false)

	val autoTunnelActive = _autoTunnelActive.asStateFlow()

	var autoTunnelService = CompletableDeferred<AutoTunnelService>()
	var backgroundService = CompletableDeferred<TunnelForegroundService>()
	var autoTunnelTile = CompletableDeferred<AutoTunnelControlTile>()
	var tunnelControlTile = CompletableDeferred<TunnelControlTile>()

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
		val settings = appDataRepository.settings.getSettings()
		appDataRepository.settings.save(settings.copy(isAutoTunnelEnabled = true))
		if (autoTunnelService.isCompleted) return _autoTunnelActive.update { true }
		kotlin.runCatching {
			startService(AutoTunnelService::class.java, background)
			autoTunnelService.await()
			autoTunnelService.getCompleted().start()
			_autoTunnelActive.update { true }
			updateAutoTunnelTile()
		}.onFailure {
			Timber.e(it)
		}
	}

	suspend fun startBackgroundService(tunnelConfig: TunnelConfig) {
		if (backgroundService.isCompleted) return
		kotlin.runCatching {
			startService(TunnelForegroundService::class.java, true)
			backgroundService.await()
			backgroundService.getCompleted().start(tunnelConfig)
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

	suspend fun toggleAutoTunnel(background: Boolean) {
		withContext(ioDispatcher) {
			if (_autoTunnelActive.value) return@withContext stopAutoTunnel()
			startAutoTunnel(background)
		}
	}

	fun updateAutoTunnelTile() {
		if (autoTunnelTile.isCompleted) {
			autoTunnelTile.getCompleted().updateTileState()
		} else {
			context.requestAutoTunnelTileServiceUpdate()
		}
	}

	fun updateTunnelTile() {
		if (tunnelControlTile.isCompleted) {
			tunnelControlTile.getCompleted().updateTileState()
		} else {
			context.requestTunnelTileServiceStateUpdate()
		}
	}

	suspend fun stopAutoTunnel() {
		withContext(ioDispatcher) {
			val settings = appDataRepository.settings.getSettings()
			appDataRepository.settings.save(settings.copy(isAutoTunnelEnabled = false))
			if (!autoTunnelService.isCompleted) return@withContext
			runCatching {
				autoTunnelService.getCompleted().stop()
				_autoTunnelActive.update { false }
				updateAutoTunnelTile()
			}.onFailure {
				Timber.e(it)
			}
		}
	}
}
