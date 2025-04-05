package com.zaneschepke.wireguardautotunnel.ui.screens.settings.appearance.language.components

import androidx.compose.runtime.Composable
import com.zaneschepke.wireguardautotunnel.ui.common.SelectedLabel
import com.zaneschepke.wireguardautotunnel.ui.common.button.SelectionItemButton
import com.zaneschepke.wireguardautotunnel.ui.state.AppUiState
import com.zaneschepke.wireguardautotunnel.viewmodel.AppViewModel
import com.zaneschepke.wireguardautotunnel.viewmodel.event.AppEvent
import java.util.*

@Composable
fun LanguageItem(locale: Locale, appUiState: AppUiState, viewModel: AppViewModel) {
	SelectionItemButton(
		buttonText = locale.getDisplayLanguage(locale).replaceFirstChar {
			if (it.isLowerCase()) it.titlecase(locale) else it.toString()
		} + if (locale.toLanguageTag().contains("-")) {
			" (${locale.getDisplayCountry(locale).replaceFirstChar {
				if (it.isLowerCase()) it.titlecase(locale) else it.toString()
			}})"
		} else {
			""
		},
		onClick = {
			viewModel.handleEvent(AppEvent.SetLocale(locale.toLanguageTag()))
		},
		trailing = {
			if (locale.toLanguageTag() == appUiState.generalState.locale) {
				SelectedLabel()
			}
		},
		ripple = false,
	)
}
