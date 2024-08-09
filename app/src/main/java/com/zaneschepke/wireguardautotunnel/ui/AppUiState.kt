package com.zaneschepke.wireguardautotunnel.ui

data class AppUiState(
	val snackbarMessage: String = "",
	val snackbarMessageConsumed: Boolean = true,
	val notificationPermissionAccepted: Boolean = false,
	val requestPermissions: Boolean = false,
)
