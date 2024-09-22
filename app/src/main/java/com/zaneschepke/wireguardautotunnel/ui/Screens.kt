package com.zaneschepke.wireguardautotunnel.ui

import kotlinx.serialization.Serializable

sealed class Screens {
	@Serializable
	data object Support : Screens()

	@Serializable
	data object Settings : Screens()

	@Serializable
	data object Main : Screens()

	@Serializable
	data class Option(
		val id: Int,
	) : Screens()

	@Serializable
	data object Lock : Screens()

	@Serializable
	data class Config(
		val id: Int,
	) : Screens()

	@Serializable
	data object Logs : Screens()
}
