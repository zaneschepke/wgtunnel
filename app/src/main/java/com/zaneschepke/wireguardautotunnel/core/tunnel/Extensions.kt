package com.zaneschepke.wireguardautotunnel.core.tunnel

import com.zaneschepke.wireguardautotunnel.domain.enums.TunnelStatus
import com.zaneschepke.wireguardautotunnel.domain.model.TunnelConf
import com.zaneschepke.wireguardautotunnel.domain.state.TunnelState
import kotlinx.coroutines.flow.MutableStateFlow

fun Map<TunnelConf, TunnelState>.allDown(): Boolean {
    return this.all { it.value.status.isDown() }
}

fun Map<TunnelConf, TunnelState>.hasActive(): Boolean {
    return this.any { it.value.status.isUp() }
}

fun Map<TunnelConf, TunnelState>.getValueById(id: Int): TunnelState? {
    val key = this.keys.find { it.id == id }
    return key?.let { this@getValueById[it] }
}

fun Map<TunnelConf, TunnelState>.getKeyById(id: Int): TunnelConf? {
    return this.keys.find { it.id == id }
}

fun Map<TunnelConf, TunnelState>.isUp(tunnelConf: TunnelConf): Boolean {
    return this.getValueById(tunnelConf.id)?.status?.isUp() ?: false
}

fun MutableStateFlow<Map<TunnelConf, TunnelState>>.exists(id: Int): Boolean {
    return this.value.any { it.key.id == id }
}

fun MutableStateFlow<Map<TunnelConf, TunnelState>>.isUp(id: Int): Boolean {
    return this.value.any { it.key.id == id && it.value.status == TunnelStatus.Up }
}

fun MutableStateFlow<Map<TunnelConf, TunnelState>>.isStarting(id: Int): Boolean {
    return this.value.any { it.key.id == id && it.value.status == TunnelStatus.Starting }
}

fun MutableStateFlow<Map<TunnelConf, TunnelState>>.findTunnel(id: Int): TunnelConf? {
    return this.value.keys.find { it.id == id }
}

private val URL_PATTERN =
    Regex("""^([a-zA-Z0-9]([a-zA-Z0-9\-]{0,61}[a-zA-Z0-9])?\.)+[a-zA-Z]{2,}:[0-9]{1,5}$""")

fun String.isUrl(): Boolean {
    return URL_PATTERN.matches(this)
}
