package com.zaneschepke.wireguardautotunnel.ui.screens.settings.advanced

import android.view.WindowManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.MainActivity
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SurfaceSelectionGroupButton
import com.zaneschepke.wireguardautotunnel.ui.screens.settings.advanced.components.RemoteControlItem
import com.zaneschepke.wireguardautotunnel.ui.state.AppUiState
import com.zaneschepke.wireguardautotunnel.viewmodel.AppViewModel

@Composable
fun SettingsAdvancedScreen(appUiState: AppUiState, viewModel: AppViewModel) {
    val context = LocalContext.current

    val activity = context as? MainActivity

    // Secure screen due to sensitive information
    DisposableEffect(Unit) {
        activity
            ?.window
            ?.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE,
            )
        onDispose { activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
    }

    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.Top),
        modifier =
            Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 24.dp)
                .padding(horizontal = 12.dp),
    ) {
        SurfaceSelectionGroupButton(listOf(RemoteControlItem(appUiState, viewModel)))
    }
}
