package com.zaneschepke.wireguardautotunnel.ui.screens.settings.appearance.language

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.ui.navigation.LocalIsAndroidTV
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.appearance.language.components.AutomaticLanguageItem
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.appearance.language.components.LanguageItem
import com.zaneschepke.wireguardautotunnel.ui.state.AppUiState
import com.zaneschepke.wireguardautotunnel.util.LocaleUtil
import com.zaneschepke.wireguardautotunnel.viewmodel.AppViewModel
import java.text.Collator
import java.util.*

@Composable
fun LanguageScreen(appUiState: AppUiState, viewModel: AppViewModel) {
    val collator = Collator.getInstance(Locale.getDefault())
    val isTv = LocalIsAndroidTV.current

    val locales =
        LocaleUtil.supportedLocales.map {
            val tag = it.replace("_", "-")
            Locale.forLanguageTag(tag)
        }

    val sortedLocales =
        remember(locales) {
            locales.sortedWith(compareBy(collator) { it.getDisplayName(it) }).toList()
        }

    LazyColumn(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
        modifier = Modifier.padding(horizontal = 12.dp),
    ) {
        item { AutomaticLanguageItem(appUiState, viewModel, isTv) }
        items(sortedLocales, key = { it }) { locale ->
            LanguageItem(locale, appUiState, viewModel, isTv)
        }
    }
}
