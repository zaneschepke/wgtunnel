package com.zaneschepke.wireguardautotunnel.core.tunnel

import com.zaneschepke.wireguardautotunnel.domain.entity.TunnelConf
import com.zaneschepke.wireguardautotunnel.domain.state.TunnelState

fun Map<TunnelConf, TunnelState>.allDown(): Boolean {
	return this.all { it.value.state.isDown() }
}

fun Map<TunnelConf, TunnelState>.hasActive(): Boolean {
	return this.any { it.value.state.isUp() }
}

fun Map<TunnelConf, TunnelState>.getValueById(id: Int): TunnelState? {
	val key = this.keys.find { it.id == id }
	return key?.let { this@getValueById[it] }
}

fun Map<TunnelConf, TunnelState>.getKeyById(id: Int): TunnelConf? {
	return this.keys.find { it.id == id }
}

fun Map<TunnelConf, TunnelState>.isUp(tunnelConf: TunnelConf): Boolean {
	return this.getValueById(tunnelConf.id)?.state?.isUp() ?: false
}
