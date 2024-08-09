package com.zaneschepke.wireguardautotunnel.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zaneschepke.wireguardautotunnel.data.repository.AppDataRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import xyz.teamgravity.pin_lock_compose.PinManager
import javax.inject.Inject

@HiltViewModel
class AppViewModel
@Inject
constructor(
	private val appDataRepository: AppDataRepository,
) : ViewModel() {

	private val _appUiState =
		MutableStateFlow(
			AppUiState(),
		)
	val appUiState = _appUiState.asStateFlow()

	fun showSnackbarMessage(message: String) {
		_appUiState.update {
			it.copy(
				snackbarMessage = message,
				snackbarMessageConsumed = false,
			)
		}
	}

	fun snackbarMessageConsumed() {
		_appUiState.update {
			it.copy(
				snackbarMessage = "",
				snackbarMessageConsumed = true,
			)
		}
	}

	fun onPinLockDisabled() = viewModelScope.launch {
		PinManager.clearPin()
		appDataRepository.appState.setPinLockEnabled(false)
	}

	fun onPinLockEnabled() = viewModelScope.launch {
		appDataRepository.appState.setPinLockEnabled(true)
	}
}
