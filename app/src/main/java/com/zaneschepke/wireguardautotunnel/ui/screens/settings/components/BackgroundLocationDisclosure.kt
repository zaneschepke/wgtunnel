package com.zaneschepke.wireguardautotunnel.ui.screens.settings.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocationOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.util.extensions.isRunningOnTv

@Composable
fun BackgroundLocationDisclosure(
	onDismiss: () -> Unit,
	onAttest: () -> Unit,
	scrollState: ScrollState,
	focusRequester: FocusRequester,
) {
	val context = LocalContext.current
	Column(
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.Top,
		modifier =
		Modifier
			.fillMaxSize()
			.verticalScroll(scrollState),
	) {
		Icon(
			Icons.Rounded.LocationOff,
			contentDescription = stringResource(id = R.string.map),
			modifier =
			Modifier
				.padding(30.dp)
				.size(128.dp),
		)
		Text(
			stringResource(R.string.prominent_background_location_title),
			textAlign = TextAlign.Center,
			modifier = Modifier.padding(30.dp),
			fontSize = 20.sp,
		)
		Text(
			stringResource(R.string.prominent_background_location_message),
			textAlign = TextAlign.Center,
			modifier = Modifier.padding(30.dp),
			fontSize = 15.sp,
		)
		Row(
			modifier =
			if (context.isRunningOnTv()) {
				Modifier
					.fillMaxWidth()
					.padding(10.dp)
			} else {
				Modifier
					.fillMaxWidth()
					.padding(30.dp)
			},
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.SpaceEvenly,
		) {
			TextButton(onClick = { onDismiss() }) {
				Text(stringResource(id = R.string.no_thanks))
			}
			TextButton(
				modifier = Modifier.focusRequester(focusRequester),
				onClick = {
					onAttest()
				},
			) {
				Text(stringResource(id = R.string.turn_on))
			}
		}
	}
}
