package com.zaneschepke.wireguardautotunnel.ui.screens.config.model

import com.wireguard.config.Interface

data class InterfaceProxy(
	val privateKey: String = "",
	val publicKey: String = "",
	val addresses: String = "",
	val dnsServers: String = "",
	val listenPort: String = "",
	val mtu: String = "",
	val junkPacketCount: String = "",
	val junkPacketMinSize: String = "",
	val junkPacketMaxSize: String = "",
	val initPacketJunkSize: String = "",
	val responsePacketJunkSize: String = "",
	val initPacketMagicHeader: String = "",
	val responsePacketMagicHeader: String = "",
	val underloadPacketMagicHeader: String = "",
	val transportPacketMagicHeader: String = "",
) {
	companion object {
		fun from(i: Interface): InterfaceProxy {
			return InterfaceProxy(
				publicKey = i.keyPair.publicKey.toBase64().trim(),
				privateKey = i.keyPair.privateKey.toBase64().trim(),
				addresses = i.addresses.joinToString(", ").trim(),
				dnsServers = listOf(
					i.dnsServers.joinToString(", ").replace("/", "").trim(),
					i.dnsSearchDomains.joinToString(", ").trim(),
				).filter { it.length > 0 } .joinToString(", "),
				listenPort =
				if (i.listenPort.isPresent) {
					i.listenPort.get().toString().trim()
				} else {
					""
				},
				mtu = if (i.mtu.isPresent) i.mtu.get().toString().trim() else "",
			)
		}

		fun from(i: org.amnezia.awg.config.Interface): InterfaceProxy {
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
				junkPacketCount =
				if (i.junkPacketCount.isPresent) {
					i.junkPacketCount.get()
						.toString()
				} else {
					""
				},
				junkPacketMinSize =
				if (i.junkPacketMinSize.isPresent) {
					i.junkPacketMinSize.get()
						.toString()
				} else {
					""
				},
				junkPacketMaxSize =
				if (i.junkPacketMaxSize.isPresent) {
					i.junkPacketMaxSize.get()
						.toString()
				} else {
					""
				},
				initPacketJunkSize =
				if (i.initPacketJunkSize.isPresent) {
					i.initPacketJunkSize.get()
						.toString()
				} else {
					""
				},
				responsePacketJunkSize =
				if (i.responsePacketJunkSize.isPresent) {
					i.responsePacketJunkSize.get()
						.toString()
				} else {
					""
				},
				initPacketMagicHeader =
				if (i.initPacketMagicHeader.isPresent) {
					i.initPacketMagicHeader.get()
						.toString()
				} else {
					""
				},
				responsePacketMagicHeader =
				if (i.responsePacketMagicHeader.isPresent) {
					i.responsePacketMagicHeader.get()
						.toString()
				} else {
					""
				},
				transportPacketMagicHeader =
				if (i.transportPacketMagicHeader.isPresent) {
					i.transportPacketMagicHeader.get()
						.toString()
				} else {
					""
				},
				underloadPacketMagicHeader =
				if (i.underloadPacketMagicHeader.isPresent) {
					i.underloadPacketMagicHeader.get()
						.toString()
				} else {
					""
				},
			)
		}
	}
}
