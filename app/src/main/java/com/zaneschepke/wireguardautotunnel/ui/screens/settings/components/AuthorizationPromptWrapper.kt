package com.zaneschepke.wireguardautotunnel.ui.screens.settings.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.common.prompt.AuthorizationPrompt
import com.zaneschepke.wireguardautotunnel.util.StringValue
import com.zaneschepke.wireguardautotunnel.viewmodel.AppViewModel
import com.zaneschepke.wireguardautotunnel.viewmodel.event.AppEvent

@Composable
fun AuthorizationPromptWrapper(
    onSuccess: () -> Unit,
    onDismiss: () -> Unit,
    viewModel: AppViewModel,
) {
    val context = LocalContext.current

    AuthorizationPrompt(
        onSuccess = { onSuccess() },
        onError = { _ ->
            onDismiss()
            viewModel.handleEvent(
                AppEvent.ShowMessage(
                    StringValue.StringResource(R.string.error_authentication_failed)
                )
            )
        },
        onFailure = {
            onDismiss()
            viewModel.handleEvent(
                AppEvent.ShowMessage(
                    StringValue.StringResource(R.string.error_authorization_failed)
                )
            )
        },
    )
}
