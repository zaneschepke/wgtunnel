package com.zaneschepke.wireguardautotunnel.util.extensions

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.navigation.NavController
import com.zaneschepke.wireguardautotunnel.WireGuardAutoTunnel
import com.zaneschepke.wireguardautotunnel.ui.Route

fun NavController.navigateAndForget(route: Route) {
	navigate(route) {
		popUpTo(0)
	}
}

fun Dp.scaledHeight(): Dp {
	return WireGuardAutoTunnel.instance.resizeHeight(this)
}

fun Dp.scaledWidth(): Dp {
	return WireGuardAutoTunnel.instance.resizeWidth(this)
}

fun TextUnit.scaled(): TextUnit {
	return WireGuardAutoTunnel.instance.resizeHeight(this)
}
