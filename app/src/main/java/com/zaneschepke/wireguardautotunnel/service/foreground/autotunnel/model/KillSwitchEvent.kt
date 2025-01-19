package com.zaneschepke.wireguardautotunnel.service.foreground.autotunnel.model

sealed class KillSwitchEvent {
	data class Start(val allowedIps: Set<String>) : KillSwitchEvent()
	data object Stop : KillSwitchEvent()
	data object DoNothing : KillSwitchEvent()
}
