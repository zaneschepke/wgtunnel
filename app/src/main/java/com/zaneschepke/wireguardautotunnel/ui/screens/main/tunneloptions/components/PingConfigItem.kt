package com.zaneschepke.wireguardautotunnel.ui.screens.main.tunneloptions.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.domain.model.TunnelConf
import com.zaneschepke.wireguardautotunnel.ui.common.button.surface.SelectionItem
import com.zaneschepke.wireguardautotunnel.ui.common.config.SubmitConfigurationTextBox
import com.zaneschepke.wireguardautotunnel.util.Constants
import com.zaneschepke.wireguardautotunnel.util.extensions.isValidIpv4orIpv6Address
import com.zaneschepke.wireguardautotunnel.viewmodel.AppViewModel
import com.zaneschepke.wireguardautotunnel.viewmodel.event.AppEvent

@Composable
fun PingConfigItem(tunnelConf: TunnelConf, viewModel: AppViewModel): SelectionItem {
    return SelectionItem(
        title = {},
        description = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                SubmitConfigurationTextBox(
                    value = tunnelConf.pingIp,
                    label = stringResource(R.string.set_custom_ping_ip),
                    hint = stringResource(R.string.default_ping_ip),
                    isErrorValue = { it?.isNotBlank() == true && !it.isValidIpv4orIpv6Address() },
                    onSubmit = { ip ->
                        viewModel.handleEvent(AppEvent.SetTunnelPingIp(tunnelConf, ip))
                    },
                )
                SubmitConfigurationTextBox(
                    value = tunnelConf.pingInterval?.let { (it / 1000).toString() },
                    label = stringResource(R.string.set_custom_ping_internal),
                    hint =
                        "(${stringResource(R.string.optional_default)} ${Constants.PING_INTERVAL / 1000})",
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done,
                        ),
                    isErrorValue = {
                        it?.toLongOrNull()?.let { value -> value >= Long.MAX_VALUE / 1000 } ?: false
                    },
                    onSubmit = { interval ->
                        viewModel.handleEvent(AppEvent.SetTunnelPingInterval(tunnelConf, interval))
                    },
                )
                SubmitConfigurationTextBox(
                    value = tunnelConf.pingCooldown?.let { (it / 1000).toString() },
                    label = stringResource(R.string.set_custom_ping_cooldown),
                    hint =
                        "(${stringResource(R.string.optional_default)} ${Constants.PING_COOLDOWN / 1000})",
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done,
                        ),
                    isErrorValue = {
                        it?.toLongOrNull()?.let { value -> value >= Long.MAX_VALUE / 1000 } ?: false
                    },
                    onSubmit = { cooldown ->
                        viewModel.handleEvent(AppEvent.SetTunnelPingCooldown(tunnelConf, cooldown))
                    },
                )
            }
        },
    )
}
