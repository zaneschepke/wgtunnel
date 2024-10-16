package com.zaneschepke.wireguardautotunnel.util.extensions

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.service.quicksettings.TileService
import android.widget.Toast
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.receiver.BackgroundActionReceiver
import com.zaneschepke.wireguardautotunnel.service.tile.AutoTunnelControlTile
import com.zaneschepke.wireguardautotunnel.service.tile.TunnelControlTile
import com.zaneschepke.wireguardautotunnel.util.Constants

fun Context.openWebUrl(url: String): Result<Unit> {
	return kotlin.runCatching {
		val webpage: Uri = Uri.parse(url)
		val intent = Intent(Intent.ACTION_VIEW, webpage).apply {
			addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
		}
		startActivity(intent)
	}.onFailure {
		showToast(R.string.no_browser_detected)
	}
}

fun Context.launchShareFile(file: Uri) {
	val shareIntent = Intent().apply {
		setAction(Intent.ACTION_SEND)
		setType("*/*")
		putExtra(Intent.EXTRA_STREAM, file)
		addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
	}
	this.startActivity(Intent.createChooser(shareIntent, ""))
}

fun Context.showToast(resId: Int) {
	Toast.makeText(
		this,
		this.getString(resId),
		Toast.LENGTH_LONG,
	).show()
}

fun Context.launchSupportEmail() {
	val intent =
		Intent(Intent.ACTION_SENDTO).apply {
			data = Uri.parse("mailto:")
			putExtra(Intent.EXTRA_EMAIL, arrayOf(getString(R.string.my_email)))
			putExtra(Intent.EXTRA_SUBJECT, getString(R.string.email_subject))
			addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
		}
	if (intent.resolveActivity(packageManager) != null) {
		startActivity(
			Intent.createChooser(intent, getString(R.string.email_chooser)).apply {
				addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
			},
		)
	} else {
		showToast(R.string.no_email_detected)
	}
}

fun Context.isRunningOnTv(): Boolean {
	return packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
}

fun Context.launchVpnSettings(): Result<Unit> {
	return kotlin.runCatching {
		val intent = Intent(Constants.VPN_SETTINGS_PACKAGE).apply {
			setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
		}
		startActivity(intent)
	}
}

fun Context.launchLocationServicesSettings(): Result<Unit> {
	return kotlin.runCatching {
		val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
			setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
		}
		startActivity(intent)
	}
}

fun Context.launchSettings(): Result<Unit> {
	return kotlin.runCatching {
		val intent = Intent(Settings.ACTION_SETTINGS).apply {
			setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
		}
		startActivity(intent)
	}
}

fun Context.launchAppSettings() {
	kotlin.runCatching {
		val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
			data = Uri.fromParts("package", packageName, null)
			setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
		}
		startActivity(intent)
	}
}

fun Context.startTunnelBackground(tunnelId: Int) {
	sendBroadcast(
		Intent(this, BackgroundActionReceiver::class.java).apply {
			action = BackgroundActionReceiver.ACTION_CONNECT
			putExtra(BackgroundActionReceiver.TUNNEL_ID_EXTRA_KEY, tunnelId)
		},
	)
}

fun Context.stopTunnelBackground(tunnelId: Int) {
	sendBroadcast(
		Intent(this, BackgroundActionReceiver::class.java).apply {
			action = BackgroundActionReceiver.ACTION_DISCONNECT
			putExtra(BackgroundActionReceiver.TUNNEL_ID_EXTRA_KEY, tunnelId)
		},
	)
}

fun Context.requestTunnelTileServiceStateUpdate() {
	TileService.requestListeningState(
		this,
		ComponentName(this, TunnelControlTile::class.java),
	)
}

fun Context.requestAutoTunnelTileServiceUpdate() {
	TileService.requestListeningState(
		this,
		ComponentName(this, AutoTunnelControlTile::class.java),
	)
}
