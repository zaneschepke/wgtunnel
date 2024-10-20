package com.zaneschepke.wireguardautotunnel.ui.screens.main

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.data.domain.Settings
import com.zaneschepke.wireguardautotunnel.data.domain.TunnelConfig
import com.zaneschepke.wireguardautotunnel.data.repository.AppDataRepository
import com.zaneschepke.wireguardautotunnel.module.IoDispatcher
import com.zaneschepke.wireguardautotunnel.service.foreground.ServiceManager
import com.zaneschepke.wireguardautotunnel.service.tunnel.TunnelService
import com.zaneschepke.wireguardautotunnel.ui.common.snackbar.SnackbarController
import com.zaneschepke.wireguardautotunnel.util.Constants
import com.zaneschepke.wireguardautotunnel.util.FileReadException
import com.zaneschepke.wireguardautotunnel.util.InvalidFileExtensionException
import com.zaneschepke.wireguardautotunnel.util.NumberUtils
import com.zaneschepke.wireguardautotunnel.util.StringValue
import com.zaneschepke.wireguardautotunnel.util.extensions.extractNameAndNumber
import com.zaneschepke.wireguardautotunnel.util.extensions.hasNumberInParentheses
import com.zaneschepke.wireguardautotunnel.util.extensions.toWgQuickString
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.InputStream
import java.util.zip.ZipInputStream
import javax.inject.Inject

@HiltViewModel
class MainViewModel
@Inject
constructor(
	private val appDataRepository: AppDataRepository,
	val tunnelService: TunnelService,
	@IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

	private fun stopWatcherService(context: Context) {
		ServiceManager.stopWatcherService(context)
	}

	fun onDelete(tunnel: TunnelConfig, context: Context) {
		viewModelScope.launch {
			val settings = appDataRepository.settings.getSettings()
			val isPrimary = tunnel.isPrimaryTunnel
			if (appDataRepository.tunnels.count() == 1 || isPrimary) {
				stopWatcherService(context)
				resetTunnelSetting(settings)
			}
			appDataRepository.tunnels.delete(tunnel)
		}
	}

	private fun resetTunnelSetting(settings: Settings) {
		saveSettings(
			settings.copy(
				isAutoTunnelEnabled = false,
				isAlwaysOnVpnEnabled = false,
			),
		)
	}

	fun onExpandedChanged(expanded: Boolean) = viewModelScope.launch {
		appDataRepository.appState.setTunnelStatsExpanded(expanded)
	}

	fun onTunnelStart(tunnelConfig: TunnelConfig) = viewModelScope.launch {
		Timber.i("Starting tunnel ${tunnelConfig.name}")
		tunnelService.startTunnel(tunnelConfig)
	}

	fun onTunnelStop(tunnel: TunnelConfig) = viewModelScope.launch {
		Timber.i("Stopping active tunnel")
		tunnelService.stopTunnel(tunnel)
	}

	private fun generateQrCodeDefaultName(config: String): String {
		return try {
			TunnelConfig.configFromAmQuick(config).peers[0].endpoint.get().host
		} catch (e: Exception) {
			Timber.e(e)
			NumberUtils.generateRandomTunnelName()
		}
	}

	private fun generateQrCodeTunnelName(config: String): String {
		var defaultName = generateQrCodeDefaultName(config)
		val lines = config.lines().toMutableList()
		val linesIterator = lines.iterator()
		while (linesIterator.hasNext()) {
			val next = linesIterator.next()
			if (next.contains(Constants.QR_CODE_NAME_PROPERTY)) {
				defaultName = next.substringAfter(Constants.QR_CODE_NAME_PROPERTY).trim()
				break
			}
		}
		return defaultName
	}

	private suspend fun makeTunnelNameUnique(name: String): String {
		return withContext(ioDispatcher) {
			val tunnels = appDataRepository.tunnels.getAll()
			var tunnelName = name
			var num = 1
			while (tunnels.any { it.name == tunnelName }) {
				tunnelName = if (!tunnelName.hasNumberInParentheses()) {
					"$name($num)"
				} else {
					val pair = tunnelName.extractNameAndNumber()
					"${pair?.first}($num)"
				}
				num++
			}
			tunnelName
		}
	}

	private suspend fun saveTunnelConfigFromStream(stream: InputStream, fileName: String) {
		val amConfig = stream.use { org.amnezia.awg.config.Config.parse(it) }
		val tunnelName = makeTunnelNameUnique(getNameFromFileName(fileName))
		saveTunnel(
			TunnelConfig(
				name = tunnelName,
				wgQuick = amConfig.toWgQuickString(),
				amQuick = amConfig.toAwgQuickString(true),
			),
		)
	}

	private fun getInputStreamFromUri(uri: Uri, context: Context): InputStream? {
		return context.applicationContext.contentResolver.openInputStream(uri)
	}

	fun onTunnelFileSelected(uri: Uri, context: Context) = viewModelScope.launch(ioDispatcher) {
		kotlin.runCatching {
			if (!isValidUriContentScheme(uri)) throw InvalidFileExtensionException
			val fileName = getFileName(context, uri)
			when (getFileExtensionFromFileName(fileName)) {
				Constants.CONF_FILE_EXTENSION ->
					saveTunnelFromConfUri(fileName, uri, context)
				Constants.ZIP_FILE_EXTENSION ->
					saveTunnelsFromZipUri(
						uri,
						context,
					)
				else -> throw InvalidFileExtensionException
			}
		}.onFailure {
			Timber.e(it)
			if (it is InvalidFileExtensionException) {
				SnackbarController.showMessage(StringValue.StringResource(R.string.error_file_extension))
			} else {
				SnackbarController.showMessage(StringValue.StringResource(R.string.error_file_format))
			}
		}
	}

	private suspend fun saveTunnelsFromZipUri(uri: Uri, context: Context) {
		ZipInputStream(getInputStreamFromUri(uri, context)).use { zip ->
			generateSequence { zip.nextEntry }
				.filterNot {
					it.isDirectory ||
						getFileExtensionFromFileName(it.name) != Constants.CONF_FILE_EXTENSION
				}
				.forEach { entry ->
					val name = getNameFromFileName(entry.name)
					val amConf = org.amnezia.awg.config.Config.parse(zip.bufferedReader())
					saveTunnel(
						TunnelConfig(
							name = makeTunnelNameUnique(name),
							wgQuick = amConf.toWgQuickString(),
							amQuick = amConf.toAwgQuickString(true),
						),
					)
				}
		}
	}

	private suspend fun saveTunnelFromConfUri(name: String, uri: Uri, context: Context) {
		val stream = getInputStreamFromUri(uri, context) ?: throw FileReadException
		saveTunnelConfigFromStream(stream, name)
	}

	fun onToggleAutoTunnelingPause() = viewModelScope.launch {
		val settings = appDataRepository.settings.getSettings()
		appDataRepository.settings.save(
			settings.copy(isAutoTunnelPaused = !settings.isAutoTunnelPaused),
		)
	}

	private fun saveTunnel(tunnelConfig: TunnelConfig) = viewModelScope.launch {
		appDataRepository.tunnels.save(tunnelConfig)
	}

	private fun getFileNameByCursor(context: Context, uri: Uri): String? {
		return context.contentResolver.query(uri, null, null, null, null)?.use {
			getDisplayNameByCursor(it)
		}
	}

	private fun getDisplayNameColumnIndex(cursor: Cursor): Int? {
		val columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
		if (columnIndex == -1) return null
		return columnIndex
	}

	private fun getDisplayNameByCursor(cursor: Cursor): String? {
		val move = cursor.moveToFirst()
		if (!move) return null
		val index = getDisplayNameColumnIndex(cursor)
		if (index == null) return index
		return cursor.getString(index)
	}

	private fun isValidUriContentScheme(uri: Uri): Boolean {
		return uri.scheme == Constants.URI_CONTENT_SCHEME
	}

	private fun getFileName(context: Context, uri: Uri): String {
		return getFileNameByCursor(context, uri) ?: NumberUtils.generateRandomTunnelName()
	}

	private fun getNameFromFileName(fileName: String): String {
		return fileName.substring(0, fileName.lastIndexOf('.'))
	}

	private fun getFileExtensionFromFileName(fileName: String): String? {
		return try {
			fileName.substring(fileName.lastIndexOf('.'))
		} catch (e: Exception) {
			Timber.e(e)
			null
		}
	}

	private fun saveSettings(settings: Settings) = viewModelScope.launch { appDataRepository.settings.save(settings) }

	fun onCopyTunnel(tunnel: TunnelConfig) = viewModelScope.launch {
		saveTunnel(
			tunnel.copy(
				id = 0,
				isPrimaryTunnel = false,
				isMobileDataTunnel = false,
				isActive = false,
				name = makeTunnelNameUnique(tunnel.name),
			),
		)
	}
}
