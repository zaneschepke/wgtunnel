package com.zaneschepke.wireguardautotunnel.util.extensions

import androidx.navigation.NavController
import com.zaneschepke.wireguardautotunnel.ui.Route

fun NavController.navigateAndForget(route: Route) {
	navigate(route) {
		popUpTo(0)
	}
}
