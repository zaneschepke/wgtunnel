package com.zaneschepke.wireguardautotunnel.ui.screens.pin

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.Route
import com.zaneschepke.wireguardautotunnel.ui.common.navigation.LocalNavController
import com.zaneschepke.wireguardautotunnel.util.StringValue
import com.zaneschepke.wireguardautotunnel.util.extensions.isRunningOnTv
import com.zaneschepke.wireguardautotunnel.viewmodel.AppViewModel
import com.zaneschepke.wireguardautotunnel.viewmodel.event.AppEvent
import xyz.teamgravity.pin_lock_compose.PinLock

@Composable
fun PinLockScreen(viewModel: AppViewModel) {
	val context = LocalContext.current
	val navController = LocalNavController.current
	PinLock(
		title = { pinExists ->
			Text(
				color = MaterialTheme.colorScheme.onSurface,
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
		backgroundColor = MaterialTheme.colorScheme.surface,
		textColor = MaterialTheme.colorScheme.onSurface,
		onPinCorrect = {
			// pin is correct, navigate or hide pin lock
			if (context.isRunningOnTv()) {
				navController.navigate(Route.Main)
			} else {
				val isPopped = navController.popBackStack()
				if (!isPopped) {
					navController.navigate(Route.Main)
				}
			}
		},
		onPinIncorrect = {
			// pin is incorrect, show error
			viewModel.handleEvent(AppEvent.ShowMessage(StringValue.StringResource(R.string.incorrect_pin)))
		},
		onPinCreated = {
			// pin created for the first time, navigate or hide pin lock
			viewModel.handleEvent(AppEvent.ShowMessage(StringValue.StringResource(R.string.pin_created)))

			viewModel.handleEvent(AppEvent.TogglePinLock)
		},
	)
}
