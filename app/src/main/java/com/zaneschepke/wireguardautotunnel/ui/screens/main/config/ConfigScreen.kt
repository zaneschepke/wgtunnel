package com.zaneschepke.wireguardautotunnel.ui.screens.main.config

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.domain.entity.TunnelConf
import com.zaneschepke.wireguardautotunnel.ui.common.navigation.TopNavBar
import com.zaneschepke.wireguardautotunnel.ui.common.prompt.AuthorizationPrompt
import com.zaneschepke.wireguardautotunnel.ui.common.snackbar.SnackbarController
import com.zaneschepke.wireguardautotunnel.ui.screens.main.config.components.AddPeerButton
import com.zaneschepke.wireguardautotunnel.ui.screens.main.config.components.InterfaceSection
import com.zaneschepke.wireguardautotunnel.ui.screens.main.config.components.PeersSection
import com.zaneschepke.wireguardautotunnel.util.extensions.scaledHeight
import com.zaneschepke.wireguardautotunnel.util.extensions.scaledWidth

@Composable
fun ConfigScreen(tunnelConf: TunnelConf?, viewModel: ConfigViewModel = hiltViewModel()) {
	val context = LocalContext.current
	val snackbar = SnackbarController.current
	val keyboardController = LocalSoftwareKeyboardController.current

	val uiState by viewModel.uiState.collectAsStateWithLifecycle()

	LaunchedEffect(tunnelConf) {
		viewModel.initFromTunnel(tunnelConf)
	}

	LaunchedEffect(uiState.message) {
		uiState.message?.let { message ->
			snackbar.showMessage(message.asString(context))
			viewModel.setMessage(null)
		}
	}

	if (uiState.showAuthPrompt) {
		AuthorizationPrompt(
			onSuccess = {
				viewModel.toggleShowAuthPrompt()
				viewModel.onAuthenticated()
			},
			onError = {
				viewModel.toggleShowAuthPrompt()
				snackbar.showMessage(
					context.getString(R.string.error_authentication_failed),
				)
			},
			onFailure = {
				viewModel.toggleShowAuthPrompt()
				snackbar.showMessage(
					context.getString(R.string.error_authorization_failed),
				)
			},
		)
	}

	Scaffold(
		topBar = {
			TopNavBar(
				title = stringResource(R.string.edit_tunnel),
				trailing = {
					IconButton(onClick = {
						keyboardController?.hide()
						viewModel.save(tunnelConf)
					}) {
						Icon(Icons.Outlined.Save, contentDescription = stringResource(R.string.save))
					}
				},
			)
		},
	) { padding ->
		Column(
			verticalArrangement = Arrangement.spacedBy(24.dp.scaledHeight(), Alignment.Top),
			modifier = Modifier
				.fillMaxSize()
				.padding(padding)
				.imePadding()
				.verticalScroll(rememberScrollState())
				.padding(top = 24.dp.scaledHeight())
				.padding(horizontal = 24.dp.scaledWidth()),
		) {
			InterfaceSection(uiState, viewModel)
			PeersSection(uiState, viewModel)
			AddPeerButton(viewModel)
		}
	}
}
