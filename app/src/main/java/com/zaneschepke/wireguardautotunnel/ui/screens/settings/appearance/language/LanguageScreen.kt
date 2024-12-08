package com.zaneschepke.wireguardautotunnel.ui.screens.settings.appearance.language

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.AppUiState
import com.zaneschepke.wireguardautotunnel.ui.AppViewModel
import com.zaneschepke.wireguardautotunnel.ui.common.SelectedLabel
import com.zaneschepke.wireguardautotunnel.ui.common.button.SelectionItemButton
import com.zaneschepke.wireguardautotunnel.ui.common.navigation.TopNavBar
import com.zaneschepke.wireguardautotunnel.util.LocaleUtil
import com.zaneschepke.wireguardautotunnel.util.extensions.scaledHeight
import com.zaneschepke.wireguardautotunnel.util.extensions.scaledWidth
import java.text.Collator
import java.util.Locale

@Composable
fun LanguageScreen(appUiState: AppUiState, appViewModel: AppViewModel) {
	val collator = Collator.getInstance(Locale.getDefault())

	val locales = LocaleUtil.supportedLocales.map {
		val tag = it.replace("_", "-")
		Locale.forLanguageTag(tag)
	}

	val sortedLocales =
		remember(locales) {
			locales.sortedWith(compareBy(collator) { it.getDisplayName(it) }).toList()
		}

	Scaffold(
		topBar = {
			TopNavBar(stringResource(R.string.language))
		},
	) { padding ->
		LazyColumn(
			horizontalAlignment = Alignment.CenterHorizontally,
			verticalArrangement = Arrangement.Top,
			modifier =
			Modifier
				.fillMaxSize().padding(padding)
				.padding(horizontal = 24.dp.scaledWidth())
		) {
			item {
				Box(modifier = Modifier.padding(top = 24.dp.scaledHeight())) {
					SelectionItemButton(
						buttonText = stringResource(R.string.automatic),
						onClick = {
							appViewModel.onLocaleChange(LocaleUtil.OPTION_PHONE_LANGUAGE)
						},
						trailing = {
							if (appUiState.generalState.locale == LocaleUtil.OPTION_PHONE_LANGUAGE) {
								SelectedLabel()
							}
						},
						ripple = false,
					)
				}
			}
			items(sortedLocales, key = { it }) { locale ->
				SelectionItemButton(
					buttonText = locale.getDisplayLanguage(locale).replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() } +
						if (locale.toLanguageTag().contains("-")) {
							" (${locale.getDisplayCountry(locale)
								.replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }})"
						} else {
							""
						},
					onClick = {
						appViewModel.onLocaleChange(locale.toLanguageTag())
					},
					trailing = {
						if (locale.toLanguageTag() == appUiState.generalState.locale) {
							SelectedLabel()
						}
					},
					ripple = false,
				)
			}
		}
	}
}
