package com.zaneschepke.wireguardautotunnel.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun PermissionRequestFailedScreen(
    padding: PaddingValues,
    onRequestAgain: () -> Unit,
    message: String,
    buttonText: String
) {
    val scope = rememberCoroutineScope()
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize().padding(padding),
    ) {
        Text(message, textAlign = TextAlign.Center, modifier = Modifier.padding(15.dp))
        Button(
            onClick = { scope.launch { onRequestAgain() } },
        ) {
            Text(buttonText)
        }
    }
}
