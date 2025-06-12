package com.zaneschepke.wireguardautotunnel.ui.screens.support

import com.zaneschepke.wireguardautotunnel.domain.model.AppUpdate
import com.zaneschepke.wireguardautotunnel.util.StringValue

data class SupportUiState(
    val appUpdate: AppUpdate? = null,
    val isLoading: Boolean = false,
    val error: StringValue? = null,
    val isUptoDate: Boolean? = null,
    val downloadProgress: Float = 0f,
)
