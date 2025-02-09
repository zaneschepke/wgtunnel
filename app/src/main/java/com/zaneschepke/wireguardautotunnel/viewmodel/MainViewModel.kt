package com.zaneschepke.wireguardautotunnel.viewmodel

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.viewModelScope
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.di.IoDispatcher
import com.zaneschepke.wireguardautotunnel.core.service.ServiceManager
import com.zaneschepke.wireguardautotunnel.core.tunnel.TunnelManager
import com.zaneschepke.wireguardautotunnel.domain.entity.AppSettings
import com.zaneschepke.wireguardautotunnel.domain.entity.TunnelConf
import com.zaneschepke.wireguardautotunnel.domain.repository.AppDataRepository
import com.zaneschepke.wireguardautotunnel.ui.common.snackbar.SnackbarController
import com.zaneschepke.wireguardautotunnel.util.Constants
import com.zaneschepke.wireguardautotunnel.util.FileReadException
import com.zaneschepke.wireguardautotunnel.util.InvalidFileExtensionException
import com.zaneschepke.wireguardautotunnel.util.NumberUtils
import com.zaneschepke.wireguardautotunnel.util.StringValue
import com.zaneschepke.wireguardautotunnel.util.extensions.extractNameAndNumber
import com.zaneschepke.wireguardautotunnel.util.extensions.hasNumberInParentheses
import com.zaneschepke.wireguardautotunnel.util.extensions.toWgQuickString
import com.zaneschepke.wireguardautotunnel.util.extensions.withData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.amnezia.awg.config.Config
import timber.log.Timber
import java.io.InputStream
import java.util.zip.ZipInputStream
import javax.inject.Inject

@HiltViewModel
class MainViewModel
@Inject
constructor(
	val tunnelManager: TunnelManager,
	@IoDispatcher private val ioDispatcher: CoroutineDispatcher,
	private val serviceManager: ServiceManager,
	appDataRepository: AppDataRepository,
) : BaseViewModel(appDataRepository) {

	private val _activeTunnels = MutableStateFlow<List<TunnelConf>>(emptyList())
	val activeTunnels = _activeTunnels.asStateFlow()

	init {
		viewModelScope.launch {
			tunnelManager.activeTunnels().collect(_activeTunnels::emit)
		}
	}

	fun onDelete(tunnel: TunnelConf) = viewModelScope.launch {
		appSettings.withData { settings ->
			tunnels.withData {
				if (it.size == 1 || tunnel.isPrimaryTunnel) {
					serviceManager.stopAutoTunnel()
					resetTunnelSetting(settings)
				}
				appDataRepository.tunnels.delete(tunnel)
			}
		}
	}

	private fun resetTunnelSetting(appSettings: AppSettings) {
		saveAppSettings(
			appSettings.copy(
				isAutoTunnelEnabled = false,
				isAlwaysOnVpnEnabled = false,
			),
		)
	}

	fun onExpandedChanged(expanded: Boolean) = viewModelScope.launch {
		appDataRepository.appState.setTunnelStatsExpanded(expanded)
	}

	fun onTunnelStart(tunnelConf: TunnelConf) = viewModelScope.launch {
		appSettings.withData {
			Timber.Forest.i("Starting tunnel ${tunnelConf.tunName}")
			tunnelManager.startTunnel(tunnelConf)
		}
	}

	fun onTunnelStop(tunnelConf: TunnelConf) = viewModelScope.launch {
		appSettings.withData {
			tunnelManager.stopTunnel(tunnelConf)
		}
	}

	private fun generateQrCodeDefaultName(config: String): String {
		return try {
			TunnelConf.configFromAmQuick(config).peers[0].endpoint.get().host
		} catch (e: Exception) {
			Timber.Forest.e(e)
			NumberUtils.generateRandomTunnelName()
		}
	}

	private suspend fun makeTunnelNameUnique(name: String): String {
		return withContext(ioDispatcher) {
			val tunnels = appDataRepository.tunnels.getAll()
			var tunnelName = name
			var num = 1
			while (tunnels.any { it.tunName == tunnelName }) {
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
		val amConfig = stream.use { Config.parse(it) }
		val tunnelName = makeTunnelNameUnique(getNameFromFileName(fileName))
		saveTunnel(
			TunnelConf(
				tunName = tunnelName,
				wgQuick = amConfig.toWgQuickString(),
				amQuick = amConfig.toAwgQuickString(true),
			),
		)
	}

	private fun getInputStreamFromUri(uri: Uri, context: Context): InputStream? {
		return context.applicationContext.contentResolver.openInputStream(uri)
	}

	fun onTunnelFileSelected(uri: Uri, context: Context) = viewModelScope.launch(ioDispatcher) {
		runCatching {
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
			Timber.Forest.e(it)
			if (it is InvalidFileExtensionException) {
				SnackbarController.Companion.showMessage(StringValue.StringResource(R.string.error_file_extension))
			} else {
				SnackbarController.Companion.showMessage(StringValue.StringResource(R.string.error_file_format))
			}
		}
	}

	fun onToggleAutoTunnel() = viewModelScope.launch {
		serviceManager.toggleAutoTunnel(false)
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
					val amConf = Config.parse(zip.bufferedReader())
					saveTunnel(
						TunnelConf(
							tunName = makeTunnelNameUnique(name),
							wgQuick = amConf.toWgQuickString(),
							amQuick = amConf.toAwgQuickString(true),
						),
					)
				}
		}
	}

	fun setBatteryOptimizeDisableShown() = viewModelScope.launch {
		appDataRepository.appState.setBatteryOptimizationDisableShown(true)
	}

	private suspend fun saveTunnelFromConfUri(name: String, uri: Uri, context: Context) {
		val stream = getInputStreamFromUri(uri, context) ?: throw FileReadException
		saveTunnelConfigFromStream(stream, name)
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
			Timber.Forest.e(e)
			null
		}
	}

	fun onCopyTunnel(tunnel: TunnelConf) = viewModelScope.launch {
		saveTunnel(
			TunnelConf(
				tunName = makeTunnelNameUnique(tunnel.tunName),
				wgQuick = tunnel.wgQuick,
				amQuick = tunnel.amQuick,
			),
		)
	}

	fun onClipboardImport(config: String) = viewModelScope.launch(ioDispatcher) {
		runCatching {
			val amConfig = TunnelConf.configFromAmQuick(config)
			val tunnelConf = TunnelConf.tunnelConfigFromAmConfig(amConfig, makeTunnelNameUnique(generateQrCodeDefaultName(config)))
			saveTunnel(tunnelConf)
		}.onFailure {
			SnackbarController.Companion.showMessage(StringValue.StringResource(R.string.error_file_format))
		}
	}
}
