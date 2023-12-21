package com.zaneschepke.wireguardautotunnel.ui.screens.support

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zaneschepke.wireguardautotunnel.repository.SettingsDoa
import com.zaneschepke.wireguardautotunnel.repository.model.Settings
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class SupportViewModel @Inject constructor(
    private val settingsRepo: SettingsDoa
) : ViewModel() {
    private val _settings = MutableStateFlow(Settings())
    val settings get() = _settings.asStateFlow()
    init {
        viewModelScope.launch(Dispatchers.IO) {
            _settings.value = settingsRepo.getAll().first()
        }
    }
}
