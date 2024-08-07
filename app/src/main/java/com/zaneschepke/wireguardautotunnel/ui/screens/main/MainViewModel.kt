package com.zaneschepke.wireguardautotunnel.ui.screens.main

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wireguard.config.Config
import com.zaneschepke.wireguardautotunnel.data.domain.Settings
import com.zaneschepke.wireguardautotunnel.data.domain.TunnelConfig
import com.zaneschepke.wireguardautotunnel.data.repository.AppDataRepository
import com.zaneschepke.wireguardautotunnel.module.IoDispatcher
import com.zaneschepke.wireguardautotunnel.service.foreground.ServiceManager
import com.zaneschepke.wireguardautotunnel.service.tunnel.TunnelService
import com.zaneschepke.wireguardautotunnel.util.Constants
import com.zaneschepke.wireguardautotunnel.util.NumberUtils
import com.zaneschepke.wireguardautotunnel.util.WgTunnelExceptions
import com.zaneschepke.wireguardautotunnel.util.extensions.toWgQuickString
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
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
	private val serviceManager: ServiceManager,
	val tunnelService: TunnelService,
	@IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
	val uiState =
		combine(
			appDataRepository.settings.getSettingsFlow(),
			appDataRepository.tunnels.getTunnelConfigsFlow(),
			tunnelService.vpnState,
		) { settings, tunnels, vpnState ->
			MainUiState(settings, tunnels, vpnState, false)
		}
			.stateIn(
				viewModelScope,
				SharingStarted.WhileSubscribed(Constants.SUBSCRIPTION_TIMEOUT),
				MainUiState(),
			)

	private fun stopWatcherService(context: Context) {
		serviceManager.stopWatcherService(context)
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

	fun onTunnelStart(tunnelConfig: TunnelConfig) = viewModelScope.launch(ioDispatcher) {
		Timber.i("Starting tunnel ${tunnelConfig.name}")
		tunnelService.startTunnel(tunnelConfig)
	}

	fun onTunnelStop(tunnel: TunnelConfig) = viewModelScope.launch(ioDispatcher) {
		Timber.i("Stopping active tunnel")
		tunnelService.stopTunnel(tunnel)
	}

	private fun validateConfigString(config: String, configType: ConfigType) {
		when (configType) {
			ConfigType.AMNEZIA -> TunnelConfig.configFromAmQuick(config)
			ConfigType.WIREGUARD -> TunnelConfig.configFromWgQuick(config)
		}
	}

	private fun generateQrCodeDefaultName(config: String, configType: ConfigType): String {
		return try {
			when (configType) {
				ConfigType.AMNEZIA -> {
					TunnelConfig.configFromAmQuick(config).peers[0].endpoint.get().host
				}

				ConfigType.WIREGUARD -> {
					TunnelConfig.configFromWgQuick(config).peers[0].endpoint.get().host
				}
			}
		} catch (e: Exception) {
			Timber.e(e)
			NumberUtils.generateRandomTunnelName()
		}
	}

	private fun generateQrCodeTunnelName(config: String, configType: ConfigType): String {
		var defaultName = generateQrCodeDefaultName(config, configType)
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

	suspend fun onTunnelQrResult(result: String, configType: ConfigType): Result<Unit> {
		return withContext(ioDispatcher) {
			try {
				validateConfigString(result, configType)
				val tunnelName =
					makeTunnelNameUnique(generateQrCodeTunnelName(result, configType))
				val tunnelConfig =
					when (configType) {
						ConfigType.AMNEZIA -> {
							TunnelConfig(
								name = tunnelName,
								amQuick = result,
								wgQuick =
								TunnelConfig.configFromAmQuick(
									result,
								).toWgQuickString(),
							)
						}

						ConfigType.WIREGUARD ->
							TunnelConfig(
								name = tunnelName,
								wgQuick = result,
							)
					}
				addTunnel(tunnelConfig)
				Result.success(Unit)
			} catch (e: Exception) {
				Timber.e(e)
				Result.failure(WgTunnelExceptions.InvalidQrCode())
			}
		}
	}

	private suspend fun makeTunnelNameUnique(name: String): String {
		return withContext(ioDispatcher) {
			val tunnels = appDataRepository.tunnels.getAll()
			var tunnelName = name
			var num = 1
			while (tunnels.any { it.name == tunnelName }) {
				tunnelName = "$name($num)"
				num++
			}
			tunnelName
		}
	}

	private fun saveTunnelConfigFromStream(stream: InputStream, fileName: String, type: ConfigType) {
		var amQuick: String? = null
		val wgQuick =
			stream.use {
				when (type) {
					ConfigType.AMNEZIA -> {
						val config = org.amnezia.awg.config.Config.parse(it)
						amQuick = config.toAwgQuickString()
						config.toWgQuickString()
					}

					ConfigType.WIREGUARD -> {
						Config.parse(it).toWgQuickString(true)
					}
				}
			}
		viewModelScope.launch {
			val tunnelName = makeTunnelNameUnique(getNameFromFileName(fileName))
			addTunnel(
				TunnelConfig(
					name = tunnelName,
					wgQuick = wgQuick,
					amQuick = amQuick ?: TunnelConfig.AM_QUICK_DEFAULT,
				),
			)
		}
	}

	private fun getInputStreamFromUri(uri: Uri, context: Context): InputStream? {
		return context.applicationContext.contentResolver.openInputStream(uri)
	}

	suspend fun onTunnelFileSelected(uri: Uri, configType: ConfigType, context: Context): Result<Unit> {
		return withContext(ioDispatcher) {
			try {
				if (isValidUriContentScheme(uri)) {
					val fileName = getFileName(context, uri)
					return@withContext when (getFileExtensionFromFileName(fileName)) {
						Constants.CONF_FILE_EXTENSION ->
							saveTunnelFromConfUri(fileName, uri, configType, context)

						Constants.ZIP_FILE_EXTENSION ->
							saveTunnelsFromZipUri(
								uri,
								configType,
								context,
							)

						else -> Result.failure(WgTunnelExceptions.InvalidFileExtension())
					}
				} else {
					Result.failure(WgTunnelExceptions.InvalidFileExtension())
				}
			} catch (e: Exception) {
				Timber.e(e)
				Result.failure(WgTunnelExceptions.FileReadFailed())
			}
		}
	}

	private suspend fun saveTunnelsFromZipUri(uri: Uri, configType: ConfigType, context: Context): Result<Unit> {
		return withContext(ioDispatcher) {
			ZipInputStream(getInputStreamFromUri(uri, context)).use { zip ->
				generateSequence { zip.nextEntry }
					.filterNot {
						it.isDirectory ||
							getFileExtensionFromFileName(it.name) != Constants.CONF_FILE_EXTENSION
					}
					.forEach {
						val name = getNameFromFileName(it.name)
						withContext(viewModelScope.coroutineContext) {
							try {
								var amQuick: String? = null
								val wgQuick =
									when (configType) {
										ConfigType.AMNEZIA -> {
											val config =
												org.amnezia.awg.config.Config.parse(
													zip,
												)
											amQuick = config.toAwgQuickString()
											config.toWgQuickString()
										}

										ConfigType.WIREGUARD -> {
											Config.parse(zip).toWgQuickString(true)
										}
									}
								addTunnel(
									TunnelConfig(
										name = makeTunnelNameUnique(name),
										wgQuick = wgQuick,
										amQuick = amQuick ?: TunnelConfig.AM_QUICK_DEFAULT,
									),
								)
								Result.success(Unit)
							} catch (e: Exception) {
								Result.failure(WgTunnelExceptions.FileReadFailed())
							}
						}
					}
				Result.success(Unit)
			}
		}
	}

	private suspend fun saveTunnelFromConfUri(name: String, uri: Uri, configType: ConfigType, context: Context): Result<Unit> {
		return withContext(ioDispatcher) {
			val stream = getInputStreamFromUri(uri, context)
			return@withContext if (stream != null) {
				try {
					saveTunnelConfigFromStream(stream, name, configType)
				} catch (e: Exception) {
					return@withContext Result.failure(WgTunnelExceptions.ConfigParseError())
				}
				Result.success(Unit)
			} else {
				Result.failure(WgTunnelExceptions.FileReadFailed())
			}
		}
	}

	private fun addTunnel(tunnelConfig: TunnelConfig) = viewModelScope.launch {
		saveTunnel(tunnelConfig)
	}

	fun pauseAutoTunneling() = viewModelScope.launch {
		appDataRepository.settings.save(
			uiState.value.settings.copy(isAutoTunnelPaused = true),
		)
	}

	fun resumeAutoTunneling() = viewModelScope.launch {
		appDataRepository.settings.save(
			uiState.value.settings.copy(isAutoTunnelPaused = false),
		)
	}

	private fun saveTunnel(tunnelConfig: TunnelConfig) = viewModelScope.launch {
		appDataRepository.tunnels.save(tunnelConfig)
	}

	private fun getFileNameByCursor(context: Context, uri: Uri): String? {
		context.contentResolver.query(uri, null, null, null, null)?.use {
			return getDisplayNameByCursor(it)
		}
		return null
	}

	private fun getDisplayNameColumnIndex(cursor: Cursor): Int? {
		val columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
		return if (columnIndex != -1) {
			return columnIndex
		} else {
			null
		}
	}

	private fun getDisplayNameByCursor(cursor: Cursor): String? {
		return if (cursor.moveToFirst()) {
			val index = getDisplayNameColumnIndex(cursor)
			if (index != null) {
				cursor.getString(index)
			} else {
				null
			}
		} else {
			null
		}
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

	fun onCopyTunnel(tunnel: TunnelConfig?) = viewModelScope.launch {
		tunnel?.let {
			saveTunnel(
				TunnelConfig(
					name = it.name.plus(NumberUtils.randomThree()),
					wgQuick = it.wgQuick,
				),
			)
		}
	}
}
