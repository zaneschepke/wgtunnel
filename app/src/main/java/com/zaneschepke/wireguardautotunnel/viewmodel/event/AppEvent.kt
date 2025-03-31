package com.zaneschepke.wireguardautotunnel.viewmodel.event

sealed class AppEvent {
	data object ToggleLocalLogging : AppEvent()
	data class SetDebounceDelay(val delay: Int) : AppEvent()
}
