package com.zaneschepke.wireguardautotunnel.ui.screens.pinlock

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.WireGuardAutoTunnel
import com.zaneschepke.wireguardautotunnel.ui.AppViewModel
import com.zaneschepke.wireguardautotunnel.ui.Screen
import com.zaneschepke.wireguardautotunnel.util.StringValue
import xyz.teamgravity.pin_lock_compose.PinLock

@Composable
fun PinLockScreen(navController: NavController, appViewModel: AppViewModel) {
	val context = LocalContext.current
	PinLock(
		title = { pinExists ->
			Text(
				text =
				if (pinExists) {
					stringResource(id = R.string.enter_pin)
				} else {
					stringResource(
						id = R.string.create_pin,
					)
				},
			)
		},
		color = MaterialTheme.colorScheme.surface,
		onPinCorrect = {
			// pin is correct, navigate or hide pin lock
			if (WireGuardAutoTunnel.isRunningOnAndroidTv()) {
				navController.navigate(Screen.Main.route)
			} else {
				val isPopped = navController.popBackStack()
				if (!isPopped) {
					navController.navigate(Screen.Main.route)
				}
			}
		},
		onPinIncorrect = {
			// pin is incorrect, show error
			appViewModel.showSnackbarMessage(
				StringValue.StringResource(R.string.incorrect_pin).asString(context),
			)
		},
		onPinCreated = {
			// pin created for the first time, navigate or hide pin lock
			appViewModel.showSnackbarMessage(
				StringValue.StringResource(R.string.pin_created).asString(context),
			)
		},
	)
}
