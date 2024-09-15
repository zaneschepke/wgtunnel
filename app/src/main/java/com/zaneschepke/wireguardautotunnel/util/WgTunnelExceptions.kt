package com.zaneschepke.wireguardautotunnel.util

import android.content.Context
import com.zaneschepke.wireguardautotunnel.R

sealed class WgTunnelExceptions : Exception() {
	abstract fun getMessage(context: Context): String

	data class ConfigExportFailed(
		private val userMessage: StringValue =
			StringValue.StringResource(
				R.string.export_configs_failed,
			),
	) : WgTunnelExceptions() {
		override fun getMessage(context: Context): String {
			return userMessage.asString(context)
		}
	}

	data class ConfigParseError(private val appendMessage: StringValue = StringValue.Empty) :
		WgTunnelExceptions() {
		override fun getMessage(context: Context): String {
			return StringValue.StringResource(R.string.config_parse_error).asString(context) + (
				if (appendMessage != StringValue.Empty) ": ${appendMessage.asString(context)}" else ""
				)
		}
	}

	data class InvalidQrCode(
		private val userMessage: StringValue =
			StringValue.StringResource(
				R.string.error_invalid_code,
			),
	) :
		WgTunnelExceptions() {
		override fun getMessage(context: Context): String {
			return userMessage.asString(context)
		}
	}

	data class InvalidFileExtension(
		private val userMessage: StringValue =
			StringValue.StringResource(
				R.string.error_file_extension,
			),
	) : WgTunnelExceptions() {
		override fun getMessage(context: Context): String {
			return userMessage.asString(context)
		}
	}

	data class FileReadFailed(
		private val userMessage: StringValue =
			StringValue.StringResource(
				R.string.error_file_format,
			),
	) :
		WgTunnelExceptions() {
		override fun getMessage(context: Context): String {
			return userMessage.asString(context)
		}
	}
}
