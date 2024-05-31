package com.zaneschepke.wireguardautotunnel.util

import android.content.Context
import com.zaneschepke.wireguardautotunnel.R

sealed class WgTunnelExceptions : Exception() {
    abstract fun getMessage(context: Context): String
    data class General(private val userMessage: StringValue) : WgTunnelExceptions() {
        override fun getMessage(context: Context): String {
            return userMessage.asString(context)
        }
    }

    data class SsidConflict(private val userMessage: StringValue = StringValue.StringResource(R.string.error_ssid_exists)) :
        WgTunnelExceptions() {
        override fun getMessage(context: Context): String {
            return userMessage.asString(context)
        }
    }

    data class ConfigExportFailed(
        private val userMessage: StringValue = StringValue.StringResource(
            R.string.export_configs_failed,
        )
    ) : WgTunnelExceptions() {
        override fun getMessage(context: Context): String {
            return userMessage.asString(context)
        }
    }

    data class ConfigParseError(private val appendMessage: StringValue = StringValue.Empty) :
        WgTunnelExceptions() {
        override fun getMessage(context: Context): String {
            return StringValue.StringResource(R.string.config_parse_error).asString(context) + (
                if (appendMessage != StringValue.Empty) ": ${appendMessage.asString(context)}" else "")
        }
    }

    data class RootDenied(private val userMessage: StringValue = StringValue.StringResource(R.string.error_root_denied)) :
        WgTunnelExceptions() {
        override fun getMessage(context: Context): String {
            return userMessage.asString(context)
        }
    }

    data class InvalidQrCode(private val userMessage: StringValue = StringValue.StringResource(R.string.error_invalid_code)) :
        WgTunnelExceptions() {
        override fun getMessage(context: Context): String {
            return userMessage.asString(context)
        }
    }

    data class InvalidFileExtension(
        private val userMessage: StringValue = StringValue.StringResource(
            R.string.error_file_extension,
        )
    ) : WgTunnelExceptions() {
        override fun getMessage(context: Context): String {
            return userMessage.asString(context)
        }
    }

    data class FileReadFailed(private val userMessage: StringValue = StringValue.StringResource(R.string.error_file_format)) :
        WgTunnelExceptions() {
        override fun getMessage(context: Context): String {
            return userMessage.asString(context)
        }
    }

    data class AuthenticationFailed(
        private val userMessage: StringValue = StringValue.StringResource(
            R.string.error_authentication_failed,
        )
    ) : WgTunnelExceptions() {
        override fun getMessage(context: Context): String {
            return userMessage.asString(context)
        }
    }

    data class AuthorizationFailed(
        private val userMessage: StringValue = StringValue.StringResource(
            R.string.error_authorization_failed,
        )
    ) : WgTunnelExceptions() {
        override fun getMessage(context: Context): String {
            return userMessage.asString(context)
        }
    }

    data class BackgroundLocationRequired(
        private val userMessage: StringValue = StringValue.StringResource(
            R.string.background_location_required,
        )
    ) : WgTunnelExceptions() {
        override fun getMessage(context: Context): String {
            return userMessage.asString(context)
        }
    }

    data class LocationServicesRequired(
        private val userMessage: StringValue = StringValue.StringResource(
            R.string.location_services_required,
        )
    ) : WgTunnelExceptions() {
        override fun getMessage(context: Context): String {
            return userMessage.asString(context)
        }
    }

    data class PreciseLocationRequired(
        private val userMessage: StringValue = StringValue.StringResource(
            R.string.precise_location_required,
        )
    ) : WgTunnelExceptions() {
        override fun getMessage(context: Context): String {
            return userMessage.asString(context)
        }
    }

    data class FileExplorerRequired(
        private val userMessage: StringValue = StringValue.StringResource(
            R.string.error_no_file_explorer,
        )
    ) : WgTunnelExceptions() {
        override fun getMessage(context: Context): String {
            return userMessage.asString(context)
        }
    }
}

