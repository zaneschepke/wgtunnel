package com.zaneschepke.wireguardautotunnel.viewmodel.event

sealed class AppEvent {
	data object ToggleLocalLogging : AppEvent()
}
