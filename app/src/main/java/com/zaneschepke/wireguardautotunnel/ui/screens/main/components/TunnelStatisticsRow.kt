package com.zaneschepke.wireguardautotunnel.ui.screens.main.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.data.domain.TunnelConfig
import com.zaneschepke.wireguardautotunnel.service.tunnel.statistics.TunnelStatistics
import com.zaneschepke.wireguardautotunnel.util.NumberUtils
import com.zaneschepke.wireguardautotunnel.util.extensions.toThreeDecimalPlaceString

@Composable
fun TunnelStatisticsRow(statistics: TunnelStatistics?, tunnelConfig: TunnelConfig) {
	val config = TunnelConfig.configFromAmQuick(tunnelConfig.wgQuick)

	Row(
		modifier =
		Modifier
			.fillMaxWidth()
			.padding(end = 10.dp, bottom = 10.dp, start = 45.dp),
		verticalAlignment = Alignment.CenterVertically,
		horizontalArrangement = Arrangement.spacedBy(30.dp, Alignment.Start),
	) {
		config.peers.forEach {
			val peerId = it.publicKey.toBase64().subSequence(0, 3).toString() + "***"
			val peerRx = statistics?.peerStats(it.publicKey)?.rxBytes ?: 0
			val peerTx = statistics?.peerStats(it.publicKey)?.txBytes ?: 0
			val peerTxMB = NumberUtils.bytesToMB(peerTx).toThreeDecimalPlaceString()
			val peerRxMB = NumberUtils.bytesToMB(peerRx).toThreeDecimalPlaceString()
			val handshake = statistics?.peerStats(it.publicKey)?.latestHandshakeEpochMillis?.let {
				if (it == 0L) {
					stringResource(R.string.never)
				} else {
					"${NumberUtils.getSecondsBetweenTimestampAndNow(it)} ${stringResource(R.string.sec)}"
				}
			} ?: stringResource(R.string.never)
			Column(
				verticalArrangement = Arrangement.spacedBy(10.dp),
			) {
				Text(stringResource(R.string.peer).lowercase() + ": $peerId", style = MaterialTheme.typography.bodySmall)
				Text("tx: $peerTxMB MB", style = MaterialTheme.typography.bodySmall)
			}
			Column(
				verticalArrangement = Arrangement.spacedBy(10.dp),
			) {
				Text(stringResource(R.string.handshake) + ": $handshake", style = MaterialTheme.typography.bodySmall)
				Text("rx: $peerRxMB MB", style = MaterialTheme.typography.bodySmall)
			}
		}
	}
}
