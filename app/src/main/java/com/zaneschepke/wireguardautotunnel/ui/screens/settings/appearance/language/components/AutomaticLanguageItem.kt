package com.zaneschepke.wireguardautotunnel.ui.screens.settings.appearance.language.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.common.button.SelectionItemButton
import com.zaneschepke.wireguardautotunnel.ui.common.label.SelectedLabel
import com.zaneschepke.wireguardautotunnel.ui.state.AppUiState
import com.zaneschepke.wireguardautotunnel.util.LocaleUtil
import com.zaneschepke.wireguardautotunnel.viewmodel.AppViewModel
import com.zaneschepke.wireguardautotunnel.viewmodel.event.AppEvent

@Composable
fun AutomaticLanguageItem(appUiState: AppUiState, viewModel: AppViewModel, isAndroidTv: Boolean) {
    Box(modifier = Modifier.padding(top = 24.dp)) {
        SelectionItemButton(
            buttonText = stringResource(R.string.automatic),
            onClick = {
                viewModel.handleEvent(AppEvent.SetLocale(LocaleUtil.OPTION_PHONE_LANGUAGE))
            },
            trailing = {
                with(appUiState.appState.locale) {
                    if (this == LocaleUtil.OPTION_PHONE_LANGUAGE || this == null) {
                        SelectedLabel()
                    }
                }
            },
            ripple = isAndroidTv,
        )
    }
}
