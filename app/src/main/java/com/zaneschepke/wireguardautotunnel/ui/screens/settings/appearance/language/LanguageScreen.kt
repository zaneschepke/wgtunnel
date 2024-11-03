package com.zaneschepke.wireguardautotunnel.ui.screens.settings.appearance.language

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.data.datastore.LocaleStorage
import com.zaneschepke.wireguardautotunnel.ui.Route
import com.zaneschepke.wireguardautotunnel.ui.common.SelectedLabel
import com.zaneschepke.wireguardautotunnel.ui.common.button.SelectionItemButton
import com.zaneschepke.wireguardautotunnel.ui.common.navigation.LocalNavController
import com.zaneschepke.wireguardautotunnel.ui.common.navigation.TopNavBar
import com.zaneschepke.wireguardautotunnel.util.LocaleUtil
import com.zaneschepke.wireguardautotunnel.util.extensions.navigateAndForget
import com.zaneschepke.wireguardautotunnel.util.extensions.scaledHeight
import com.zaneschepke.wireguardautotunnel.util.extensions.scaledWidth
import timber.log.Timber
import java.text.Collator
import java.util.Locale

@Composable
fun LanguageScreen(localeStorage: LocaleStorage) {
	val navController = LocalNavController.current

	val context = LocalContext.current

	val collator = Collator.getInstance(Locale.getDefault())

	val currentLocale = remember { mutableStateOf(LocaleUtil.OPTION_PHONE_LANGUAGE) }

	val locales = LocaleUtil.supportedLocales.map {
		val tag = it.replace("_", "-")
		Locale.forLanguageTag(tag)
	}

	val sortedLocales =
		remember(locales) {
			locales.sortedWith(compareBy(collator) { it.getDisplayName(it) }).toList()
		}

	LaunchedEffect(Unit) {
		currentLocale.value = localeStorage.getPreferredLocale()
	}

	fun onChangeLocale(locale: String) {
		Timber.d("Setting preferred locale: $locale")
		localeStorage.setPreferredLocale(locale)
		LocaleUtil.applyLocalizedContext(context, locale)
		navController.navigateAndForget(Route.Main)
	}

	Scaffold(
		topBar = {
			TopNavBar(stringResource(R.string.language))
		},
	) {
		LazyColumn(
			horizontalAlignment = Alignment.CenterHorizontally,
			verticalArrangement = Arrangement.Top,
			modifier =
			Modifier
				.fillMaxSize().padding(it)
				.padding(horizontal = 24.dp.scaledWidth()).windowInsetsPadding(WindowInsets.navigationBars),
		) {
			item {
				Box(modifier = Modifier.padding(top = 24.dp.scaledHeight())) {
					SelectionItemButton(
						buttonText = stringResource(R.string.automatic),
						onClick = {
							onChangeLocale(LocaleUtil.OPTION_PHONE_LANGUAGE)
						},
						trailing = {
							if (currentLocale.value == LocaleUtil.OPTION_PHONE_LANGUAGE) {
								SelectedLabel()
							}
						},
						ripple = false,
					)
				}
			}
			items(sortedLocales, key = { it }) { locale ->
				SelectionItemButton(
					buttonText = locale.getDisplayLanguage(locale).capitalize(locale) +
						if (locale.toLanguageTag().contains("-")) " (${locale.getDisplayCountry(locale).capitalize(locale)})" else "",
					onClick = {
						onChangeLocale(locale.toLanguageTag())
					},
					trailing = {
						if (locale.toLanguageTag() == currentLocale.value) {
							SelectedLabel()
						}
					},
					ripple = false,
				)
			}
		}
	}
}
