package com.zaneschepke.wireguardautotunnel.ui.common.navigation

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.focus.FocusRequester
import androidx.navigation.NavHostController

val LocalNavController = compositionLocalOf<NavHostController> {
	error("NavController was not provided")
}

val LocalFocusRequester = compositionLocalOf<FocusRequester> { error("FocusRequester is not provided") }
