package com.zaneschepke.wireguardautotunnel.service.tunnel.model

enum class TunnelState {
	UP,
	DOWN,
	;

	fun isDown(): Boolean {
		return this == DOWN
	}

	fun isUp(): Boolean {
		return this == UP
	}
}
