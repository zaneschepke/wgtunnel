package com.zaneschepke.wireguardautotunnel.ui.screens.support

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zaneschepke.wireguardautotunnel.data.repository.SettingsRepository
import com.zaneschepke.wireguardautotunnel.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class SupportViewModel
@Inject
constructor(settingsRepository: SettingsRepository) :
	ViewModel() {
	val uiState =
		settingsRepository
			.getSettingsFlow()
			.map { SupportUiState(it) }
			.stateIn(
				viewModelScope,
				SharingStarted.WhileSubscribed(Constants.SUBSCRIPTION_TIMEOUT),
				SupportUiState(),
			)
}
