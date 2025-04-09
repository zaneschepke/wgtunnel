package com.zaneschepke.wireguardautotunnel.ui.screens.main.config.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.common.config.ConfigurationTextBox
import com.zaneschepke.wireguardautotunnel.ui.state.PeerProxy

@Composable
fun PeerFields(
    peer: PeerProxy,
    onPeerChange: (PeerProxy) -> Unit,
    showAuthPrompt: () -> Unit,
    isAuthenticated: Boolean,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() })
    val keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)

    ConfigurationTextBox(
        value = peer.publicKey,
        onValueChange = { onPeerChange(peer.copy(publicKey = it)) },
        label = stringResource(R.string.public_key),
        hint = stringResource(R.string.base64_key),
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        visualTransformation =
            if (isAuthenticated) VisualTransformation.None else PasswordVisualTransformation(),
        value = peer.preSharedKey,
        enabled = isAuthenticated,
        onValueChange = { onPeerChange(peer.copy(preSharedKey = it)) },
        label = {
            Text(
                stringResource(R.string.preshared_key),
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        placeholder = {
            Text(stringResource(R.string.optional), style = MaterialTheme.typography.bodyMedium)
        },
        modifier = Modifier.fillMaxWidth().clickable { if (!isAuthenticated) showAuthPrompt() },
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = true,
    )
    OutlinedTextField(
        value = peer.persistentKeepalive,
        onValueChange = { onPeerChange(peer.copy(persistentKeepalive = it)) },
        label = {
            Text(
                stringResource(R.string.persistent_keepalive),
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        placeholder = {
            Text(stringResource(R.string.optional), style = MaterialTheme.typography.bodyMedium)
        },
        trailingIcon = {
            Text(
                stringResource(R.string.seconds),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(end = 10.dp),
            )
        },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
    )
    ConfigurationTextBox(
        value = peer.endpoint,
        onValueChange = { onPeerChange(peer.copy(endpoint = it)) },
        label = stringResource(R.string.endpoint),
        hint = stringResource(R.string.endpoint).lowercase(),
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = peer.allowedIps,
        onValueChange = { onPeerChange(peer.copy(allowedIps = it)) },
        label = {
            Text(stringResource(R.string.allowed_ips), style = MaterialTheme.typography.bodyMedium)
        },
        placeholder = {
            Text(
                stringResource(R.string.comma_separated_list),
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
    )
}
