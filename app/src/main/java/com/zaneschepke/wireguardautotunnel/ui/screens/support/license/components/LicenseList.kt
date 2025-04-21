package com.zaneschepke.wireguardautotunnel.ui.screens.support.license.components

import LicenseFileEntry
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LicenseList(licenses: List<LicenseFileEntry>) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        items(licenses) { entry ->
            Column(modifier = Modifier.padding(bottom = 12.dp)) {
                Text(
                    text = "${entry.name} (${entry.version})",
                    style = MaterialTheme.typography.titleSmall,
                )

                entry.spdxLicenses.forEach { license ->
                    Text(
                        text = license.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                entry.scm?.url?.let { scmUrl ->
                    Text(
                        text = scmUrl,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(top = 4.dp),
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
        }
    }
}
