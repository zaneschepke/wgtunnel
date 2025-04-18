package com.zaneschepke.wireguardautotunnel.ui.state

import com.zaneschepke.wireguardautotunnel.domain.entity.TunnelConf
import com.zaneschepke.wireguardautotunnel.util.StringValue

data class AppViewState(
    val isConfigChanged: Boolean = false,
    val errorMessage: StringValue? = null,
    val popBackStack: Boolean = false,
    val isAppReady: Boolean = false,
    val showBottomSheet: Boolean = false,
    val selectedTunnels: List<TunnelConf> = emptyList(),
    val requestVpnPermission: Boolean = false,
    val requestBatteryPermission: Boolean = false,
    val showModal: ModalType = ModalType.NONE,
) {
    enum class ModalType {
        NONE,
        DELETE,
        INFO,
    }
}
