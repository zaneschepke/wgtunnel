package com.zaneschepke.wireguardautotunnel.ui.state

import com.zaneschepke.wireguardautotunnel.util.StringValue

data class AppState(
	val isConfigChanged: Boolean = false,
	val errorMessage: StringValue? = null,
	val popBackStack: Boolean = false,
	val isAppReady: Boolean = false,
)
