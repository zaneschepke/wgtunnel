package com.zaneschepke.wireguardautotunnel.ui.screens.main.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.iamageo.multifablibrary.FabIcon
import com.iamageo.multifablibrary.FabOption
import com.iamageo.multifablibrary.MultiFabItem
import com.iamageo.multifablibrary.MultiFloatingActionButton
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.screens.main.ConfigType
import com.zaneschepke.wireguardautotunnel.util.extensions.isRunningOnTv

@Composable
fun ScrollDismissMultiFab(focusRequester: FocusRequester, isVisible: Boolean, onFabItemClicked: (fabItem: MultiFabItem) -> Unit) {
	// Nested scroll for control FAB

	val context = LocalContext.current

	AnimatedVisibility(
		visible = isVisible,
		enter = slideInVertically(initialOffsetY = { it * 2 }),
		exit = slideOutVertically(targetOffsetY = { it * 2 }),
		modifier =
		Modifier
			.focusRequester(focusRequester)
			.focusGroup(),
	) {
		val secondaryColor = MaterialTheme.colorScheme.secondary
		val tvFobColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
		val fobColor =
			if (context.isRunningOnTv()) tvFobColor else secondaryColor
		val fobIconColor =
			if (context.isRunningOnTv()) Color.White else MaterialTheme.colorScheme.background
		MultiFloatingActionButton(
			fabIcon =
			FabIcon(
				iconRes = R.drawable.add,
				iconResAfterRotate = R.drawable.close,
				iconRotate = 180f,
			),
			fabOption =
			FabOption(
				iconTint = fobIconColor,
				backgroundTint = fobColor,
			),
			itemsMultiFab =
			listOf(
				MultiFabItem(
					label = {
						Text(
							stringResource(id = R.string.amnezia),
							color = Color.White,
							textAlign = TextAlign.Center,
							modifier = Modifier.padding(end = 10.dp),
						)
					},
					modifier =
					Modifier
						.size(40.dp),
					icon = R.drawable.add,
					value = ConfigType.AMNEZIA.name,
					miniFabOption =
					FabOption(
						backgroundTint = fobColor,
						fobIconColor,
					),
				),
				MultiFabItem(
					label = {
						Text(
							stringResource(id = R.string.wireguard),
							color = Color.White,
							textAlign = TextAlign.Center,
							modifier = Modifier.padding(end = 10.dp),
						)
					},
					icon = R.drawable.add,
					value = ConfigType.WIREGUARD.name,
					miniFabOption =
					FabOption(
						backgroundTint = fobColor,
						fobIconColor,
					),
				),
			),
			onFabItemClicked = {
				onFabItemClicked(it)
			},
			shape = RoundedCornerShape(16.dp),
		)
	}
}
