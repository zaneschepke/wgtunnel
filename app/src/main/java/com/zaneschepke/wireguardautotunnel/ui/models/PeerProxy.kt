package com.zaneschepke.wireguardautotunnel.ui.models

import com.wireguard.config.Peer

data class PeerProxy(
    var publicKey : String = "",
    var preSharedKey : String = "",
    var persistentKeepalive : String = "",
    var endpoint : String = "",
    var allowedIps: String = IPV4_WILDCARD.joinToString(", ").trim()
){
    companion object {
        fun from(peer : Peer) : PeerProxy {
            return PeerProxy(
                publicKey = peer.publicKey.toBase64(),
                preSharedKey = if(peer.preSharedKey.isPresent) peer.preSharedKey.get().toBase64().trim() else "",
                persistentKeepalive = if(peer.persistentKeepalive.isPresent) peer.persistentKeepalive.get().toString().trim() else "",
                endpoint = if(peer.endpoint.isPresent) peer.endpoint.get().toString().trim() else "",
                allowedIps = peer.allowedIps.joinToString(", ").trim()
            )
        }
        val IPV4_PUBLIC_NETWORKS = setOf(
            "0.0.0.0/5", "8.0.0.0/7", "11.0.0.0/8", "12.0.0.0/6", "16.0.0.0/4", "32.0.0.0/3",
            "64.0.0.0/2", "128.0.0.0/3", "160.0.0.0/5", "168.0.0.0/6", "172.0.0.0/12",
            "172.32.0.0/11", "172.64.0.0/10", "172.128.0.0/9", "173.0.0.0/8", "174.0.0.0/7",
            "176.0.0.0/4", "192.0.0.0/9", "192.128.0.0/11", "192.160.0.0/13", "192.169.0.0/16",
            "192.170.0.0/15", "192.172.0.0/14", "192.176.0.0/12", "192.192.0.0/10",
            "193.0.0.0/8", "194.0.0.0/7", "196.0.0.0/6", "200.0.0.0/5", "208.0.0.0/4"
        )
        val IPV4_WILDCARD = setOf("0.0.0.0/0")
    }
}