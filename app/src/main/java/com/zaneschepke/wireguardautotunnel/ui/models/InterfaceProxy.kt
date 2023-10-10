package com.zaneschepke.wireguardautotunnel.ui.models

import com.wireguard.config.Interface
import com.wireguard.config.Peer

data class InterfaceProxy(
    var privateKey : String = "",
    var publicKey : String = "",
    var addresses : String = "",
    var dnsServers : String = "",
    var listenPort : String = "",
    var mtu : String = "",
){
    companion object {
        fun from(i : Interface) : InterfaceProxy {
            return InterfaceProxy(
                publicKey = i.keyPair.publicKey.toBase64(),
                privateKey = i.keyPair.privateKey.toBase64(),
                addresses = i.addresses.joinToString(","),
                dnsServers = i.dnsServers.joinToString(",").replace("/", ""),
                listenPort = if(i.listenPort.isPresent) i.listenPort.get().toString() else "",
                mtu = if(i.mtu.isPresent) i.mtu.get().toString() else ""
            )
        }
    }
}