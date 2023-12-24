package com.zaneschepke.wireguardautotunnel.util

import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.WireGuardAutoTunnel

sealed class Event {

    abstract val message: String

    sealed class Error : Event() {
        data object None : Error() {
            override val message: String
                get() = WireGuardAutoTunnel.instance.getString(R.string.error_none)
        }
        data object SsidConflict : Error() {
            override val message: String
                get() = WireGuardAutoTunnel.instance.getString(R.string.error_ssid_exists)
        }
        data object RootDenied : Error() {
            override val message: String
                get() = WireGuardAutoTunnel.instance.getString(R.string.error_root_denied)
        }
        data class General(val customMessage: String) : Error() {
            override val message: String
                get() = customMessage
        }
        data class Exception(val exception : kotlin.Exception) : Error() {
            override val message: String
                get() = exception.message ?: WireGuardAutoTunnel.instance.getString(R.string.unknown_error)
        }
        data object InvalidQrCode : Error() {
            override val message: String
                get() = WireGuardAutoTunnel.instance.getString(R.string.error_invalid_code)
        }
        data object InvalidFileExtension : Error() {
            override val message: String
                get() = WireGuardAutoTunnel.instance.getString(R.string.error_file_extension)
        }
        data object FileReadFailed : Error() {
            override val message: String
                get() = WireGuardAutoTunnel.instance.getString(R.string.error_file_extension)
        }
        data object AuthenticationFailed : Error() {
            override val message: String
                get() = WireGuardAutoTunnel.instance.getString(R.string.error_authentication_failed)
        }
        data object AuthorizationFailed : Error() {
            override val message: String
                get() = WireGuardAutoTunnel.instance.getString(R.string.error_authorization_failed)
        }
        data object BackgroundLocationRequired : Error() {
            override val message: String
                get() = WireGuardAutoTunnel.instance.getString(R.string.background_location_required)
        }
        data object LocationServicesRequired : Error() {
            override val message: String
                get() = WireGuardAutoTunnel.instance.getString(R.string.location_services_required)
        }
        data object PreciseLocationRequired : Error() {
            override val message: String
                get() = WireGuardAutoTunnel.instance.getString(R.string.precise_location_required)
        }
        data object FileExplorerRequired : Error() {
            override val message: String
                get() = WireGuardAutoTunnel.instance.getString(R.string.error_no_file_explorer)
        }
    }
    sealed class Message : Event() {
        data object ConfigSaved: Message() {
            override val message: String
                get() = WireGuardAutoTunnel.instance.getString(R.string.config_changes_saved)
        }
        data object ConfigsExported: Message() {
            override val message: String
                get() = WireGuardAutoTunnel.instance.getString(R.string.exported_configs_message)
        }
        data object TunnelOffAction: Message() {
            override val message: String
                get() = WireGuardAutoTunnel.instance.getString(R.string.turn_off_tunnel)
        }
        data object TunnelOnAction: Message() {
            override val message: String
                get() = WireGuardAutoTunnel.instance.getString(R.string.turn_on_tunnel)
        }
        data object AutoTunnelOffAction: Message() {
            override val message: String
                get() = WireGuardAutoTunnel.instance.getString(R.string.turn_off_auto)
        }
    }
}