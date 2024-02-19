package com.zaneschepke.wireguardautotunnel.data

object Queries {
    fun createDefaultSettings() : String {
        return """
            INSERT INTO Settings (is_tunnel_enabled,
                    is_tunnel_on_mobile_data_enabled,
                    trusted_network_ssids,
                    default_tunnel,
                    is_always_on_vpn_enabled,
                    is_tunnel_on_ethernet_enabled,
                    is_shortcuts_enabled,
                    is_battery_saver_enabled,
                    is_tunnel_on_wifi_enabled,
                    is_kernel_enabled,
                    is_restore_on_boot_enabled,
                    is_multi_tunnel_enabled)
                    VALUES
                    ('false',
                    'false',
                    '[trustedSSID1,trustedSSID2]',
                     NULL,
                    'false',
                    'false',
                    'false',
                    'false',
                    'false',
                    'false',
                    'false',
                    'false')
        """.trimIndent()
    }
}
