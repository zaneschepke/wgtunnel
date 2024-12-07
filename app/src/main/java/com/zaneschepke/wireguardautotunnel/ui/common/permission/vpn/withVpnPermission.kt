package com.zaneschepke.wireguardautotunnel.ui.common.permission.vpn

import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity.RESULT_OK
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

@Composable
inline fun <T> withVpnPermission(crossinline onSuccess: (t: T) -> Unit): (t: T) -> Unit {
	val context = LocalContext.current

	var showVpnPermissionDialog by remember { mutableStateOf(false) }

	val vpnActivity =
		rememberLauncherForActivityResult(
			ActivityResultContracts.StartActivityForResult(),
			onResult = {
				if (it.resultCode != RESULT_OK) showVpnPermissionDialog = true
			},
		)

	VpnDeniedDialog(showVpnPermissionDialog, onDismiss = { showVpnPermissionDialog = false })

	return {
		val intent = VpnService.prepare(context)
		if (intent != null) {
			vpnActivity.launch(intent)
		} else {
			onSuccess(it)
		}
	}
}
