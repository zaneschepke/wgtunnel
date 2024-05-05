package com.zaneschepke.wireguardautotunnel.service.tunnel

import com.wireguard.android.backend.Tunnel

enum class TunnelState {
    UP,
    DOWN,
    TOGGLE;

    fun toWgState() : Tunnel.State {
        return when(this) {
            UP -> Tunnel.State.UP
            DOWN -> Tunnel.State.DOWN
            TOGGLE -> Tunnel.State.TOGGLE
        }
    }

    fun toAmState() : org.amnezia.awg.backend.Tunnel.State {
        return when(this) {
            UP -> org.amnezia.awg.backend.Tunnel.State.UP
            DOWN -> org.amnezia.awg.backend.Tunnel.State.DOWN
            TOGGLE -> org.amnezia.awg.backend.Tunnel.State.TOGGLE
        }
    }

    companion object {
        fun from(state: Tunnel.State) : TunnelState {
            return when(state) {
                Tunnel.State.DOWN -> DOWN
                Tunnel.State.TOGGLE -> TOGGLE
                Tunnel.State.UP -> UP
            }
        }
        fun from(state: org.amnezia.awg.backend.Tunnel.State) : TunnelState {
            return when(state) {
                org.amnezia.awg.backend.Tunnel.State.DOWN -> DOWN
                org.amnezia.awg.backend.Tunnel.State.TOGGLE -> TOGGLE
                org.amnezia.awg.backend.Tunnel.State.UP -> UP
            }
        }
    }
}
