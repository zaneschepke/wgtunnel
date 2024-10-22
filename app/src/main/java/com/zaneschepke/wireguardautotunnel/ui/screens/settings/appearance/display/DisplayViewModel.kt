package com.zaneschepke.wireguardautotunnel.ui.screens.settings.appearance.display

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zaneschepke.wireguardautotunnel.data.repository.AppStateRepository
import com.zaneschepke.wireguardautotunnel.ui.theme.Theme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DisplayViewModel
@Inject
constructor(
	private val appStateRepository: AppStateRepository
) : ViewModel() {

	fun onThemeChange(theme: Theme) = viewModelScope.launch {
		appStateRepository.setTheme(theme)
	}
}
