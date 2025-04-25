package com.zaneschepke.wireguardautotunnel.ui.screens.settings.appearance.display

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.common.button.IconSurfaceButton
import com.zaneschepke.wireguardautotunnel.ui.state.AppUiState
import com.zaneschepke.wireguardautotunnel.ui.theme.Theme
import com.zaneschepke.wireguardautotunnel.viewmodel.AppViewModel
import com.zaneschepke.wireguardautotunnel.viewmodel.event.AppEvent

@Composable
fun DisplayScreen(appUiState: AppUiState, viewModel: AppViewModel) {
    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.Top),
        modifier = Modifier.fillMaxSize().padding(top = 24.dp).padding(horizontal = 24.dp),
    ) {
        enumValues<Theme>().forEach {
            val title =
                when (it) {
                    Theme.DARK -> stringResource(R.string.dark)
                    Theme.LIGHT -> stringResource(R.string.light)
                    Theme.AUTOMATIC -> stringResource(R.string.automatic)
                    Theme.DYNAMIC -> stringResource(R.string.dynamic)
                    Theme.DARKER -> stringResource(R.string.darker)
                    Theme.AMOLED -> stringResource(R.string.amoled)
                }
            IconSurfaceButton(
                title = title,
                onClick = { viewModel.handleEvent(AppEvent.SetTheme(it)) },
                selected = appUiState.appState.theme == it,
            )
        }
    }
}
