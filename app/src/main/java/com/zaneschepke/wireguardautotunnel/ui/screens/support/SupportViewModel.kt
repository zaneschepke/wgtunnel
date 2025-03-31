package com.zaneschepke.wireguardautotunnel.ui.screens.support

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zaneschepke.logcatter.LogReader
import com.zaneschepke.wireguardautotunnel.domain.repository.AppDataRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SupportViewModel @Inject constructor(
	private val appDataRepository: AppDataRepository,
	private val logReader: LogReader,
) : ViewModel() {

	fun onToggleLocalLogging() = viewModelScope.launch {
		val enabled = appDataRepository.appState.isLocalLogsEnabled()
		appDataRepository.appState.setLocalLogsEnabled(!enabled)
		if (!enabled) {
			logReader.stop()
		} else {
			logReader.start()
		}
	}
}
