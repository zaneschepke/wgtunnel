package com.zaneschepke.wireguardautotunnel.ui.state

import com.wireguard.config.Interface
import com.zaneschepke.wireguardautotunnel.util.extensions.joinAndTrim
import com.zaneschepke.wireguardautotunnel.util.extensions.toTrimmedList

data class InterfaceProxy(
    val privateKey: String = "",
    val publicKey: String = "",
    val addresses: String = "",
    val dnsServers: String = "",
    val listenPort: String = "",
    val mtu: String = "",
    val includedApplications: MutableSet<String> = mutableSetOf(),
    val excludedApplications: MutableSet<String> = mutableSetOf(),
    val junkPacketCount: String = "",
    val junkPacketMinSize: String = "",
    val junkPacketMaxSize: String = "",
    val initPacketJunkSize: String = "",
    val responsePacketJunkSize: String = "",
    val initPacketMagicHeader: String = "",
    val responsePacketMagicHeader: String = "",
    val underloadPacketMagicHeader: String = "",
    val transportPacketMagicHeader: String = "",
    val preUp: String = "",
    val postUp: String = "",
    val preDown: String = "",
    val postDown: String = "",
) {

    fun toWgInterface(): Interface {
        return Interface.Builder()
            .apply {
                parseAddresses(addresses)
                parsePrivateKey(privateKey)
                if (dnsServers.isNotBlank()) parseDnsServers(dnsServers)
                if (mtu.isNotBlank()) parseMtu(mtu)
                if (listenPort.isNotBlank()) parseListenPort(listenPort)
                includeApplications(includedApplications)
                excludeApplications(excludedApplications)
                preUp.toTrimmedList().forEach { parsePreUp(it) }
                postUp.toTrimmedList().forEach { parsePostUp(it) }
                preDown.toTrimmedList().forEach { parsePreDown(it) }
                postDown.toTrimmedList().forEach { parsePostDown(it) }
            }
            .build()
    }

    fun toAmneziaCompatibilityConfig(): InterfaceProxy {
        return copy(
            junkPacketCount = "4",
            junkPacketMinSize = "40",
            junkPacketMaxSize = "70",
            initPacketJunkSize = "0",
            responsePacketJunkSize = "0",
            initPacketMagicHeader = "1",
            responsePacketMagicHeader = "2",
            underloadPacketMagicHeader = "3",
            transportPacketMagicHeader = "4",
        )
    }

    fun resetAmneziaProperties(): InterfaceProxy {
        return copy(
            junkPacketCount = "",
            junkPacketMinSize = "",
            junkPacketMaxSize = "",
            initPacketJunkSize = "",
            responsePacketJunkSize = "",
            initPacketMagicHeader = "",
            responsePacketMagicHeader = "",
            underloadPacketMagicHeader = "",
            transportPacketMagicHeader = "",
        )
    }

    fun isAmneziaCompatibilityModeSet(): Boolean {
        return with(initPacketJunkSize.toIntOrNull()) { this == 0 || this == null } &&
            with(responsePacketJunkSize.toIntOrNull()) { this == 0 || this == null } &&
            initPacketMagicHeader.toLongOrNull() == 1L &&
            responsePacketMagicHeader.toLongOrNull() == 2L &&
            underloadPacketMagicHeader.toLongOrNull() == 3L &&
            transportPacketMagicHeader.toLongOrNull() == 4L
    }

    fun toAmInterface(): org.amnezia.awg.config.Interface {
        return org.amnezia.awg.config.Interface.Builder()
            .apply {
                parseAddresses(addresses)
                parsePrivateKey(privateKey)
                if (dnsServers.isNotBlank()) parseDnsServers(dnsServers)
                if (mtu.isNotBlank()) parseMtu(mtu)
                if (listenPort.isNotBlank()) parseListenPort(listenPort)
                includeApplications(includedApplications)
                excludeApplications(excludedApplications)
                preUp.toTrimmedList().forEach { parsePreUp(it) }
                postUp.toTrimmedList().forEach { parsePostUp(it) }
                preDown.toTrimmedList().forEach { parsePreDown(it) }
                postDown.toTrimmedList().forEach { parsePostDown(it) }
                if (junkPacketCount.isNotBlank()) parseJunkPacketCount(junkPacketCount)
                if (junkPacketMinSize.isNotBlank()) parseJunkPacketMinSize(junkPacketMinSize)
                if (junkPacketMaxSize.isNotBlank()) parseJunkPacketMaxSize(junkPacketMaxSize)
                if (initPacketJunkSize.isNotBlank()) parseInitPacketJunkSize(initPacketJunkSize)
                if (responsePacketJunkSize.isNotBlank())
                    parseResponsePacketJunkSize(responsePacketJunkSize)
                if (initPacketMagicHeader.isNotBlank())
                    parseInitPacketMagicHeader(initPacketMagicHeader)
                if (responsePacketMagicHeader.isNotBlank())
                    parseResponsePacketMagicHeader(responsePacketMagicHeader)
                if (underloadPacketMagicHeader.isNotBlank())
                    parseUnderloadPacketMagicHeader(underloadPacketMagicHeader)
                if (transportPacketMagicHeader.isNotBlank())
                    parseTransportPacketMagicHeader(transportPacketMagicHeader)
            }
            .build()
    }

    companion object {
        fun from(i: Interface): InterfaceProxy {
            return InterfaceProxy(
                publicKey = i.keyPair.publicKey.toBase64().trim(),
                privateKey = i.keyPair.privateKey.toBase64().trim(),
                addresses = i.addresses.joinToString(", ").trim(),
                dnsServers =
                    listOf(
                            i.dnsServers.joinToString(", ").replace("/", "").trim(),
                            i.dnsSearchDomains.joinAndTrim(),
                        )
                        .filter { it.isNotEmpty() }
                        .joinToString(", "),
                listenPort =
                    if (i.listenPort.isPresent) {
                        i.listenPort.get().toString().trim()
                    } else {
                        ""
                    },
                mtu = if (i.mtu.isPresent) i.mtu.get().toString().trim() else "",
                includedApplications = i.includedApplications.toMutableSet(),
                excludedApplications = i.excludedApplications.toMutableSet(),
                preUp = i.preUp.joinAndTrim(),
                postUp = i.postUp.joinAndTrim(),
                preDown = i.preDown.joinAndTrim(),
                postDown = i.postDown.joinAndTrim(),
            )
        }

        fun from(i: org.amnezia.awg.config.Interface): InterfaceProxy {
            return InterfaceProxy(
                publicKey = i.keyPair.publicKey.toBase64().trim(),
                privateKey = i.keyPair.privateKey.toBase64().trim(),
                addresses = i.addresses.joinToString(", ").trim(),
                dnsServers =
                    (i.dnsServers + i.dnsSearchDomains).joinToString(", ").replace("/", "").trim(),
                listenPort =
                    if (i.listenPort.isPresent) {
                        i.listenPort.get().toString().trim()
                    } else {
                        ""
                    },
                mtu = if (i.mtu.isPresent) i.mtu.get().toString().trim() else "",
                includedApplications = i.includedApplications.toMutableSet(),
                excludedApplications = i.excludedApplications.toMutableSet(),
                preUp = i.preUp.joinAndTrim(),
                postUp = i.postUp.joinAndTrim(),
                preDown = i.preDown.joinAndTrim(),
                postDown = i.postDown.joinAndTrim(),
                junkPacketCount =
                    if (i.junkPacketCount.isPresent) {
                        i.junkPacketCount.get().toString()
                    } else {
                        ""
                    },
                junkPacketMinSize =
                    if (i.junkPacketMinSize.isPresent) {
                        i.junkPacketMinSize.get().toString()
                    } else {
                        ""
                    },
                junkPacketMaxSize =
                    if (i.junkPacketMaxSize.isPresent) {
                        i.junkPacketMaxSize.get().toString()
                    } else {
                        ""
                    },
                initPacketJunkSize =
                    if (i.initPacketJunkSize.isPresent) {
                        i.initPacketJunkSize.get().toString()
                    } else {
                        ""
                    },
                responsePacketJunkSize =
                    if (i.responsePacketJunkSize.isPresent) {
                        i.responsePacketJunkSize.get().toString()
                    } else {
                        ""
                    },
                initPacketMagicHeader =
                    if (i.initPacketMagicHeader.isPresent) {
                        i.initPacketMagicHeader.get().toString()
                    } else {
                        ""
                    },
                responsePacketMagicHeader =
                    if (i.responsePacketMagicHeader.isPresent) {
                        i.responsePacketMagicHeader.get().toString()
                    } else {
                        ""
                    },
                transportPacketMagicHeader =
                    if (i.transportPacketMagicHeader.isPresent) {
                        i.transportPacketMagicHeader.get().toString()
                    } else {
                        ""
                    },
                underloadPacketMagicHeader =
                    if (i.underloadPacketMagicHeader.isPresent) {
                        i.underloadPacketMagicHeader.get().toString()
                    } else {
                        ""
                    },
            )
        }
    }
}
