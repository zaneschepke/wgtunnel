package com.zaneschepke.wireguardautotunnel.ui.screens.autotunnel.disclosure.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.common.button.ForwardButton
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItem
import com.zaneschepke.wireguardautotunnel.util.extensions.launchAppSettings
import com.zaneschepke.wireguardautotunnel.viewmodel.AppViewModel
import com.zaneschepke.wireguardautotunnel.viewmodel.event.AppEvent

@Composable
fun AppSettingsItem(viewModel: AppViewModel): SelectionItem {
    val context = LocalContext.current
    return SelectionItem(
        leadingIcon = Icons.Outlined.LocationOn,
        title = {
            Text(
                text = stringResource(R.string.launch_app_settings),
                style = MaterialTheme.typography.bodyLarge.copy(MaterialTheme.colorScheme.onSurface),
            )
        },
        trailing = {
            ForwardButton {
                context.launchAppSettings().also {
                    viewModel.handleEvent(AppEvent.SetLocationDisclosureShown)
                }
            }
        },
        onClick = {
            context.launchAppSettings().also {
                viewModel.handleEvent(AppEvent.SetLocationDisclosureShown)
            }
        },
    )
}
