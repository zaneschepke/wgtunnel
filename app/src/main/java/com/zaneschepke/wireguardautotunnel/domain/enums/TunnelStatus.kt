package com.zaneschepke.wireguardautotunnel.domain.enums

enum class TunnelStatus {
	UP,
	DOWN,
	STARTING
	;

	fun isDown(): Boolean {
		return this == DOWN
	}

	fun isUp(): Boolean {
		return this == UP
	}
}
