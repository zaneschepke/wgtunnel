package com.zaneschepke.wireguardautotunnel.ui.common.functions

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import com.zaneschepke.wireguardautotunnel.util.Constants
import com.zaneschepke.wireguardautotunnel.util.extensions.isRunningOnTv

@Composable
fun rememberFileImportLauncherForResult(onNoFileExplorer: () -> Unit, onData: (data: Uri) -> Unit): ManagedActivityResultLauncher<String, Uri?> {
	return rememberLauncherForActivityResult(
		object : ActivityResultContracts.GetContent() {
			override fun createIntent(context: Context, input: String): Intent {
				val intent = super.createIntent(context, input).apply {
					type = if (context.isRunningOnTv()) {
						Constants.ALLOWED_TV_FILE_TYPES
					} else {
						Constants.ALL_FILE_TYPES
					}
				}

				/* AndroidTV now comes with stubs that do nothing but display a Toast less helpful than
				 * what we can do, so detect this and throw an exception that we can catch later. */
				val activitiesToResolveIntent =
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
						context.packageManager.queryIntentActivities(
							intent,
							PackageManager.ResolveInfoFlags.of(
								PackageManager.MATCH_DEFAULT_ONLY.toLong(),
							),
						)
					} else {
						context.packageManager.queryIntentActivities(
							intent,
							PackageManager.MATCH_DEFAULT_ONLY,
						)
					}
				if (
					activitiesToResolveIntent.all {
						val name = it.activityInfo.packageName
						name.startsWith(Constants.GOOGLE_TV_EXPLORER_STUB) ||
							name.startsWith(Constants.ANDROID_TV_EXPLORER_STUB)
					}
				) {
					onNoFileExplorer()
				}
				return intent
			}
		},
	) { data ->
		if (data == null) return@rememberLauncherForActivityResult
		onData(data)
	}
}
