package com.zaneschepke.wireguardautotunnel.ui

import kotlinx.serialization.Serializable

sealed class Route {
	@Serializable
	data object Support : Route()

	@Serializable
	data object Settings : Route()

	@Serializable
	data object AutoTunnel : Route()

	@Serializable
	data object LocationDisclosure : Route()

	@Serializable
	data object Appearance : Route()

	@Serializable
	data object Display : Route()

	@Serializable
	data object KillSwitch : Route()

	@Serializable
	data object Language : Route()

	@Serializable
	data object Main : Route()

	@Serializable
	data class Option(
		val id: Int,
	) : Route()

	@Serializable
	data object Lock : Route()

	@Serializable
	data object Scanner : Route()

	@Serializable
	data class Config(
		val id: Int,
	) : Route()

	@Serializable
	data object Logs : Route()
}
