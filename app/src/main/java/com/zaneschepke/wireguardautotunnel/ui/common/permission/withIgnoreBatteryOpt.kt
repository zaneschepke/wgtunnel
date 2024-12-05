package com.zaneschepke.wireguardautotunnel.ui.common.permission

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.zaneschepke.wireguardautotunnel.util.extensions.isBatteryOptimizationsDisabled

@Composable
inline fun withIgnoreBatteryOpt(ignore: Boolean, crossinline callback: () -> Unit): () -> Unit {
	val context = LocalContext.current
	val batteryActivity =
		rememberLauncherForActivityResult(
			ActivityResultContracts.StartActivityForResult(),
		) { result: ActivityResult ->
			// we only ask once
			callback()
		}
	return {
		if (ignore || context.isBatteryOptimizationsDisabled()) {
			callback()
		} else {
			batteryActivity.launch(
				Intent().apply {
					action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
					data = Uri.parse("package:${context.packageName}")
				},
			)
		}
	}
}
