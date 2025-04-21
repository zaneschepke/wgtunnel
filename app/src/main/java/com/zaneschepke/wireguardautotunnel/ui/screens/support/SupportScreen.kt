package com.zaneschepke.wireguardautotunnel.ui.screens.support

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.common.SectionDivider
import com.zaneschepke.wireguardautotunnel.ui.common.label.GroupLabel
import com.zaneschepke.wireguardautotunnel.ui.screens.support.components.ContactSupportOptions
import com.zaneschepke.wireguardautotunnel.ui.screens.support.components.GeneralSupportOptions
import com.zaneschepke.wireguardautotunnel.ui.screens.support.components.VersionLabel

@Composable
fun SupportScreen() {
    val context = LocalContext.current

    Column(
        modifier =
            Modifier.fillMaxSize()
                .padding(vertical = 24.dp)
                .padding(horizontal = 12.dp)
                .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.Top),
    ) {
        GroupLabel(
            stringResource(R.string.thank_you),
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        GeneralSupportOptions(context)
        SectionDivider()
        ContactSupportOptions(context)
        VersionLabel()
    }
}
