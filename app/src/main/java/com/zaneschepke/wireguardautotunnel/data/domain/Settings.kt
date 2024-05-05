package com.zaneschepke.wireguardautotunnel.data.domain

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Settings(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "is_tunnel_enabled") val isAutoTunnelEnabled: Boolean = false,
    @ColumnInfo(name = "is_tunnel_on_mobile_data_enabled")
    val isTunnelOnMobileDataEnabled: Boolean = false,
    @ColumnInfo(name = "trusted_network_ssids")
    val trustedNetworkSSIDs: MutableList<String> = mutableListOf(),
    @ColumnInfo(name = "is_always_on_vpn_enabled") val isAlwaysOnVpnEnabled: Boolean = false,
    @ColumnInfo(name = "is_tunnel_on_ethernet_enabled")
    val isTunnelOnEthernetEnabled: Boolean = false,
    @ColumnInfo(
        name = "is_shortcuts_enabled",
        defaultValue = "false",
    )
    val isShortcutsEnabled: Boolean = false,
    @ColumnInfo(
        name = "is_tunnel_on_wifi_enabled",
        defaultValue = "false",
    )
    val isTunnelOnWifiEnabled: Boolean = false,
    @ColumnInfo(
        name = "is_kernel_enabled",
        defaultValue = "false",
    )
    val isKernelEnabled: Boolean = false,
    @ColumnInfo(
        name = "is_restore_on_boot_enabled",
        defaultValue = "false",
    )
    val isRestoreOnBootEnabled: Boolean = false,
    @ColumnInfo(
        name = "is_multi_tunnel_enabled",
        defaultValue = "false",
    )
    val isMultiTunnelEnabled: Boolean = false,
    @ColumnInfo(
        name = "is_auto_tunnel_paused",
        defaultValue = "false",
    )
    val isAutoTunnelPaused: Boolean = false,
    @ColumnInfo(
        name = "is_ping_enabled",
        defaultValue = "false",
    )
    val isPingEnabled: Boolean = false,
    @ColumnInfo(
        name = "is_amnezia_enabled",
        defaultValue = "false",
    )
    val isAmneziaEnabled: Boolean = false,
)
