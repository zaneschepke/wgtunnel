package com.zaneschepke.wireguardautotunnel.core.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.VpnService
import com.zaneschepke.wireguardautotunnel.WireGuardAutoTunnel
import com.zaneschepke.wireguardautotunnel.core.service.autotunnel.AutoTunnelService
import com.zaneschepke.wireguardautotunnel.di.ApplicationScope
import com.zaneschepke.wireguardautotunnel.di.IoDispatcher
import com.zaneschepke.wireguardautotunnel.di.MainDispatcher
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber

class ServiceManager @Inject constructor(
	private val context: Context,
	@IoDispatcher private val ioDispatcher: CoroutineDispatcher,
	@ApplicationScope private val applicationScope: CoroutineScope,
	@MainDispatcher private val mainDispatcher: CoroutineDispatcher,
	private val appDataRepository: AppDataRepository,
) {

	private val autoTunnelMutex = Mutex()

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

	fun hasVpnPermission(): Boolean {
		return VpnService.prepare(context) == null
	}

	suspend fun startAutoTunnel() {
		autoTunnelMutex.withLock {
			val settings = appDataRepository.settings.get()
			appDataRepository.settings.save(settings.copy(isAutoTunnelEnabled = true))
			if (autoTunnelService.isCompleted) {
				_autoTunnelActive.update { true }
				return
			}
			runCatching {
				autoTunnelService = CompletableDeferred()
				startService(AutoTunnelService::class.java, !WireGuardAutoTunnel.isForeground())
				_autoTunnelActive.update { true }
			}.onFailure {
				Timber.e(it)
				_autoTunnelActive.update { false }
			}
			withContext(mainDispatcher) { updateAutoTunnelTile() }
		}
	}

	suspend fun stopAutoTunnel() {
		autoTunnelMutex.withLock {
			val settings = appDataRepository.settings.get()
			appDataRepository.settings.save(settings.copy(isAutoTunnelEnabled = false))
			if (!autoTunnelService.isCompleted) return
			runCatching {
				val service = autoTunnelService.await()
				service.stop()
				_autoTunnelActive.update { false }
				autoTunnelService = CompletableDeferred()
			}.onFailure {
				Timber.e(it)
			}
			withContext(mainDispatcher) { updateAutoTunnelTile() }
		}
	}

	fun startTunnelForegroundService() {
		if (backgroundService.isCompleted) return
		runCatching {
			backgroundService = CompletableDeferred()
			startService(TunnelForegroundService::class.java, !WireGuardAutoTunnel.isForeground())
		}.onFailure {
			Timber.e(it)
		}
	}

	suspend fun stopTunnelForegroundService() {
		if (!backgroundService.isCompleted) return
		runCatching {
			val service = backgroundService.await()
			service.stop()
			backgroundService = CompletableDeferred()
		}.onFailure {
			Timber.e(it)
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
}
