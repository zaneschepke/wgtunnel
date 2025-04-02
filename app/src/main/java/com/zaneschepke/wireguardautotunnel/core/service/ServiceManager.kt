package com.zaneschepke.wireguardautotunnel.core.service

import android.app.Service
import android.content.Context
import android.content.Intent
import com.zaneschepke.wireguardautotunnel.WireGuardAutoTunnel
import com.zaneschepke.wireguardautotunnel.core.service.autotunnel.AutoTunnelService
import com.zaneschepke.wireguardautotunnel.di.ApplicationScope
import com.zaneschepke.wireguardautotunnel.di.IoDispatcher
import com.zaneschepke.wireguardautotunnel.domain.entity.TunnelConf
import com.zaneschepke.wireguardautotunnel.domain.repository.AppDataRepository
import com.zaneschepke.wireguardautotunnel.util.extensions.requestAutoTunnelTileServiceUpdate
import com.zaneschepke.wireguardautotunnel.util.extensions.requestTunnelTileServiceStateUpdate
import jakarta.inject.Inject
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber

class ServiceManager @Inject constructor(
	private val context: Context,
	@IoDispatcher private val ioDispatcher: CoroutineDispatcher,
	@ApplicationScope private val applicationScope: CoroutineScope,
	private val appDataRepository: AppDataRepository,
) {

	private val _autoTunnelActive = MutableStateFlow(false)
	val autoTunnelActive = _autoTunnelActive.asStateFlow()

	var autoTunnelService = CompletableDeferred<AutoTunnelService>()
	var backgroundService = CompletableDeferred<TunnelForegroundService>()

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

	fun startAutoTunnel() {
		applicationScope.launch(ioDispatcher) {
			val settings = appDataRepository.settings.get()
			appDataRepository.settings.save(settings.copy(isAutoTunnelEnabled = true))
			if (autoTunnelService.isCompleted) {
				_autoTunnelActive.update { true }
				return@launch
			}
			runCatching {
				autoTunnelService = CompletableDeferred()
				startService(AutoTunnelService::class.java, !WireGuardAutoTunnel.isForeground())
				_autoTunnelActive.update { true }
			}.onFailure {
				Timber.e(it)
				_autoTunnelActive.update { false }
			}
		}
		updateAutoTunnelTile()
	}

	fun startTunnelForegroundService(tunnelConf: TunnelConf) {
		applicationScope.launch(ioDispatcher) {
			if (backgroundService.isCompleted) return@launch
			runCatching {
				backgroundService = CompletableDeferred()
				startService(TunnelForegroundService::class.java, !WireGuardAutoTunnel.isForeground())
				val service = withTimeoutOrNull(SERVICE_START_TIMEOUT) { backgroundService.await() }
					?: throw IllegalStateException("Background service start timed out")
				service.start(tunnelConf)
			}.onFailure {
				Timber.e(it)
			}
		}
	}

	fun updateTunnelForegroundServiceNotification(tunnelConf: TunnelConf) {
		applicationScope.launch(ioDispatcher) {
			if (!backgroundService.isCompleted) return@launch
			runCatching {
				val service = backgroundService.await()
				service.start(tunnelConf)
			}.onFailure {
				Timber.e(it)
			}
		}
	}

	fun stopTunnelForegroundService() {
		applicationScope.launch(ioDispatcher) {
			if (!backgroundService.isCompleted) return@launch
			runCatching {
				val service = backgroundService.await()
				service.stop()
				backgroundService = CompletableDeferred()
			}.onFailure {
				Timber.e(it)
			}
		}
	}

	fun toggleAutoTunnel() {
		applicationScope.launch(ioDispatcher) {
			if (_autoTunnelActive.value) stopAutoTunnel() else startAutoTunnel()
		}
	}

	fun updateAutoTunnelTile() {
		context.requestAutoTunnelTileServiceUpdate()
	}

	fun updateTunnelTile() {
		context.requestTunnelTileServiceStateUpdate()
	}

	fun stopAutoTunnel() {
		applicationScope.launch(ioDispatcher) {
			val settings = appDataRepository.settings.get()
			appDataRepository.settings.save(settings.copy(isAutoTunnelEnabled = false))
			if (!autoTunnelService.isCompleted) return@launch
			runCatching {
				val service = autoTunnelService.await()
				service.stop()
				_autoTunnelActive.update { false }
				autoTunnelService = CompletableDeferred()
				updateAutoTunnelTile()
			}.onFailure {
				Timber.e(it)
			}
		}
	}

	companion object {
		const val SERVICE_START_TIMEOUT = 5_000L
	}
}
