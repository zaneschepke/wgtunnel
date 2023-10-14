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
        private fun String.removeWhiteSpaces() = replace("\\s".toRegex(), "")
        fun from(i : Interface) : InterfaceProxy {
            return InterfaceProxy(
                publicKey = i.keyPair.publicKey.toBase64().removeWhiteSpaces(),
                privateKey = i.keyPair.privateKey.toBase64().removeWhiteSpaces(),
                addresses = i.addresses.joinToString(",").removeWhiteSpaces(),
                dnsServers = i.dnsServers.joinToString(",").replace("/", "").removeWhiteSpaces(),
                listenPort = if(i.listenPort.isPresent) i.listenPort.get().toString().removeWhiteSpaces() else "",
                mtu = if(i.mtu.isPresent) i.mtu.get().toString().removeWhiteSpaces() else ""
            )
        }
    }
}