package com.zaneschepke.wireguardautotunnel.util.extensions

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
import android.widget.Toast
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.receiver.BackgroundActionReceiver
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

fun Context.showToast(resId: Int) {
	Toast.makeText(
		this,
		this.getString(resId),
		Toast.LENGTH_LONG,
	).show()
}

fun Context.launchSupportEmail(): Result<Unit> {
	return runCatching {
		val intent =
			Intent(Intent.ACTION_SENDTO).apply {
				type = Constants.EMAIL_MIME_TYPE
				putExtra(Intent.EXTRA_EMAIL, arrayOf(getString(R.string.my_email)))
				putExtra(Intent.EXTRA_SUBJECT, getString(R.string.email_subject))
				addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
			}
		startActivity(
			Intent.createChooser(intent, getString(R.string.email_chooser)).apply {
				addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
			},
		)
	}.onFailure {
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

fun Context.openAppSetting() {
	kotlin.runCatching {
		val intentSettings = Intent(ACTION_APPLICATION_DETAILS_SETTINGS)
		intentSettings.data = Uri.fromParts("package", packageName, null)
		startActivity(intentSettings)
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