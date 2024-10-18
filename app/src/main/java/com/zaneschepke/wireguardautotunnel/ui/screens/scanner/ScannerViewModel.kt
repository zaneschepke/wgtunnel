package com.zaneschepke.wireguardautotunnel.ui.screens.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.data.domain.TunnelConfig
import com.zaneschepke.wireguardautotunnel.data.repository.AppDataRepository
import com.zaneschepke.wireguardautotunnel.module.IoDispatcher
import com.zaneschepke.wireguardautotunnel.ui.common.snackbar.SnackbarController
import com.zaneschepke.wireguardautotunnel.util.NumberUtils
import com.zaneschepke.wireguardautotunnel.util.StringValue
import com.zaneschepke.wireguardautotunnel.util.extensions.toWgQuickString
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ScannerViewModel @Inject
constructor(
	private val appDataRepository: AppDataRepository,
	@IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

	private val _success = MutableSharedFlow<Boolean>()
	val success = _success.asSharedFlow()

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

	fun onTunnelQrResult(result: String) = viewModelScope.launch(ioDispatcher) {
		kotlin.runCatching {
			val amConfig = TunnelConfig.configFromAmQuick(result)
			val amQuick = amConfig.toAwgQuickString(true)
			val wgQuick = amConfig.toWgQuickString()
			val tunnelName = makeTunnelNameUnique(generateQrCodeDefaultName(result))
			val tunnelConfig = TunnelConfig(name = tunnelName, wgQuick = wgQuick, amQuick = amQuick)
			appDataRepository.tunnels.save(tunnelConfig)
			_success.emit(true)
		}.onFailure {
			_success.emit(false)
			Timber.e(it)
			SnackbarController.showMessage(StringValue.StringResource(R.string.error_invalid_code))
		}
	}

	private fun generateQrCodeDefaultName(config: String): String {
		return try {
			TunnelConfig.configFromAmQuick(config).peers[0].endpoint.get().host
		} catch (e: Exception) {
			Timber.e(e)
			NumberUtils.generateRandomTunnelName()
		}
	}
}
