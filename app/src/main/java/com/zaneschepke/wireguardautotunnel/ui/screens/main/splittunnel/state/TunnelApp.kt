package com.zaneschepke.wireguardautotunnel.ui.screens.main.splittunnel.state

data class TunnelApp(val name: String, val `package`: String)

typealias SplitTunnelApps = List<Pair<TunnelApp, Boolean>>
