package com.zaneschepke.wireguardautotunnel.ui

import androidx.lifecycle.ViewModel
import com.zaneschepke.wireguardautotunnel.data.SettingsDao
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ActivityViewModel @Inject constructor(
    private val settingsRepo: SettingsDao,
) : ViewModel() {

}
