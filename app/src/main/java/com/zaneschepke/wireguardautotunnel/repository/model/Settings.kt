package com.zaneschepke.wireguardautotunnel.repository.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Settings(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "is_tunnel_enabled") var isAutoTunnelEnabled: Boolean = false,
    @ColumnInfo(name = "is_tunnel_on_mobile_data_enabled") var isTunnelOnMobileDataEnabled: Boolean = false,
    @ColumnInfo(name = "trusted_network_ssids") var trustedNetworkSSIDs: MutableList<String> = mutableListOf(),
    @ColumnInfo(name = "default_tunnel") var defaultTunnel: String? = null,
    @ColumnInfo(name = "is_always_on_vpn_enabled") var isAlwaysOnVpnEnabled: Boolean = false,
    @ColumnInfo(name = "is_tunnel_on_ethernet_enabled") var isTunnelOnEthernetEnabled: Boolean = false,
    @ColumnInfo(
        name = "is_shortcuts_enabled",
        defaultValue = "false"
    ) var isShortcutsEnabled: Boolean = false,
    @ColumnInfo(
        name = "is_battery_saver_enabled",
        defaultValue = "false"
    ) var isBatterySaverEnabled: Boolean = false,
    @ColumnInfo(
        name = "is_tunnel_on_wifi_enabled",
        defaultValue = "false"
    ) var isTunnelOnWifiEnabled: Boolean = false,
    @ColumnInfo(
        name = "is_kernel_enabled",
        defaultValue = "false"
    ) var isKernelEnabled: Boolean = false,
    @ColumnInfo(
    name = "is_restore_on_boot_enabled",
    defaultValue = "false"
    ) var isRestoreOnBootEnabled: Boolean = false,
    @ColumnInfo(
    name = "is_multi_tunnel_enabled",
    defaultValue = "false"
    ) var isMultiTunnelEnabled: Boolean = false
) {
    fun isTunnelConfigDefault(tunnelConfig: TunnelConfig): Boolean {
        return if (defaultTunnel != null) {
            val defaultConfig = TunnelConfig.from(defaultTunnel!!)
            (tunnelConfig.id == defaultConfig.id)
        } else {
            false
        }
    }
}
