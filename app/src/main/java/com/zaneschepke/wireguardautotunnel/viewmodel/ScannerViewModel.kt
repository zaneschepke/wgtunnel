package com.zaneschepke.wireguardautotunnel.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.di.IoDispatcher
import com.zaneschepke.wireguardautotunnel.domain.entity.TunnelConf
import com.zaneschepke.wireguardautotunnel.domain.repository.AppDataRepository
import com.zaneschepke.wireguardautotunnel.ui.common.snackbar.SnackbarController
import com.zaneschepke.wireguardautotunnel.util.NumberUtils
import com.zaneschepke.wireguardautotunnel.util.StringValue
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
			while (tunnels.any { it.tunName == tunnelName }) {
				tunnelName = "$name($num)"
				num++
			}
			tunnelName
		}
	}

	fun onTunnelQrResult(result: String) = viewModelScope.launch(ioDispatcher) {
		runCatching {
			val amConfig = TunnelConf.configFromAmQuick(result)
			val tunnelConfig = TunnelConf.tunnelConfigFromAmConfig(amConfig, makeTunnelNameUnique(generateQrCodeDefaultName(result)))
			appDataRepository.tunnels.save(tunnelConfig)
			_success.emit(true)
		}.onFailure {
			_success.emit(false)
			Timber.Forest.e(it)
			SnackbarController.showMessage(StringValue.StringResource(R.string.error_invalid_code))
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
}
