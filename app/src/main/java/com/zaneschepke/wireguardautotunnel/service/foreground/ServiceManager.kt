package com.zaneschepke.wireguardautotunnel.service.foreground

import android.app.Service
import android.content.Context
import android.content.Intent
import timber.log.Timber

class ServiceManager {
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

	fun startWatcherServiceForeground(context: Context) {
		actionOnService(
			Action.START_FOREGROUND,
			context,
			AutoTunnelService::class.java,
		)
	}

	fun startWatcherService(context: Context) {
		actionOnService(
			Action.START,
			context,
			AutoTunnelService::class.java,
		)
	}

	fun stopWatcherService(context: Context) {
		actionOnService(
			Action.STOP,
			context,
			AutoTunnelService::class.java,
		)
	}

	fun startTunnelBackgroundService(context: Context) {
		actionOnService(
			Action.START_FOREGROUND,
			context,
			TunnelBackgroundService::class.java,
		)
	}

	fun stopTunnelBackgroundService(context: Context) {
		actionOnService(
			Action.STOP,
			context,
			TunnelBackgroundService::class.java
		)
	}
}
