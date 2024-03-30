package com.zaneschepke.wireguardautotunnel.ui.models

import com.wireguard.config.Interface

data class InterfaceProxy(
    var privateKey: String = "",
    var publicKey: String = "",
    var addresses: String = "",
    var dnsServers: String = "",
    var listenPort: String = "",
    var mtu: String = ""
) {
    companion object {
        fun from(i: Interface): InterfaceProxy {
            return InterfaceProxy(
                publicKey = i.keyPair.publicKey.toBase64().trim(),
                privateKey = i.keyPair.privateKey.toBase64().trim(),
                addresses = i.addresses.joinToString(", ").trim(),
                dnsServers = i.dnsServers.joinToString(", ").replace("/", "").trim(),
                listenPort =
                if (i.listenPort.isPresent) {
                    i.listenPort.get().toString().trim()
                } else {
                    ""
                },
                mtu = if (i.mtu.isPresent) i.mtu.get().toString().trim() else "",
            )
        }
    }
}
