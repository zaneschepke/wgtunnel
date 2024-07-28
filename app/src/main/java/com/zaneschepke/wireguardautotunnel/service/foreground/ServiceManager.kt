package com.zaneschepke.wireguardautotunnel.service.foreground

import android.app.Service
import android.content.Context
import android.content.Intent
import com.zaneschepke.wireguardautotunnel.data.repository.AppDataRepository
import com.zaneschepke.wireguardautotunnel.module.IoDispatcher
import com.zaneschepke.wireguardautotunnel.util.Constants
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import timber.log.Timber

class ServiceManager(
	private val appDataRepository: AppDataRepository,
	@IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
	private fun <T : Service> actionOnService(action: Action, context: Context, cls: Class<T>, extras: Map<String, Int>? = null) {
		val intent =
			Intent(context, cls).also {
				it.action = action.name
				extras?.forEach { (k, v) -> it.putExtra(k, v) }
			}
		intent.component?.javaClass
		try {
			when (action) {
				Action.START_FOREGROUND, Action.STOP_FOREGROUND ->
					context.startForegroundService(
						intent,
					)

				Action.START, Action.STOP -> context.startService(intent)
			}
		} catch (e: Exception) {
			Timber.e(e.message)
		}
	}

	suspend fun startVpnService(context: Context, tunnelId: Int? = null, isManualStart: Boolean = false) {
		if (isManualStart) onManualStart(tunnelId)
		actionOnService(
			Action.START,
			context,
			WireGuardTunnelService::class.java,
			tunnelId?.let { mapOf(Constants.TUNNEL_EXTRA_KEY to it) },
		)
	}

	suspend fun stopVpnServiceForeground(context: Context, isManualStop: Boolean = false) {
		withContext(ioDispatcher) {
			if (isManualStop) onManualStop()
			Timber.i("Stopping vpn service")
			actionOnService(
				Action.STOP_FOREGROUND,
				context,
				WireGuardTunnelService::class.java,
			)
		}
	}

	suspend fun stopVpnService(context: Context, isManualStop: Boolean = false) {
		withContext(ioDispatcher) {
			if (isManualStop) onManualStop()
			Timber.i("Stopping vpn service")
			actionOnService(
				Action.STOP,
				context,
				WireGuardTunnelService::class.java,
			)
		}
	}

	private suspend fun onManualStop() {
		appDataRepository.appState.setManualStop()
	}

	private suspend fun onManualStart(tunnelId: Int?) {
		tunnelId?.let {
			appDataRepository.appState.setTunnelRunningFromManualStart(it)
		}
	}

	suspend fun startVpnServiceForeground(context: Context, tunnelId: Int? = null, isManualStart: Boolean = false) {
		withContext(ioDispatcher) {
			if (isManualStart) onManualStart(tunnelId)
			actionOnService(
				Action.START_FOREGROUND,
				context,
				WireGuardTunnelService::class.java,
				tunnelId?.let { mapOf(Constants.TUNNEL_EXTRA_KEY to it) },
			)
		}
	}

	fun startWatcherServiceForeground(context: Context) {
		actionOnService(
			Action.START_FOREGROUND,
			context,
			WireGuardConnectivityWatcherService::class.java,
		)
	}

	fun startWatcherService(context: Context) {
		actionOnService(
			Action.START,
			context,
			WireGuardConnectivityWatcherService::class.java,
		)
	}

	fun stopWatcherService(context: Context) {
		actionOnService(
			Action.STOP,
			context,
			WireGuardConnectivityWatcherService::class.java,
		)
	}
}
