package com.zaneschepke.wireguardautotunnel.ui.common

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.service.tunnel.statistics.TunnelStatistics
import com.zaneschepke.wireguardautotunnel.util.NumberUtils
import com.zaneschepke.wireguardautotunnel.util.extensions.toThreeDecimalPlaceString

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RowListItem(
	icon: @Composable () -> Unit,
	text: String,
	onHold: () -> Unit,
	onClick: () -> Unit,
	rowButton: @Composable () -> Unit,
	expanded: Boolean,
	statistics: TunnelStatistics?,
	focusRequester: FocusRequester,
) {
	Box(
		modifier =
		Modifier
			.focusRequester(focusRequester)
			.animateContentSize()
			.clip(RoundedCornerShape(30.dp))
			.combinedClickable(
				onClick = { onClick() },
				onLongClick = { onHold() },
			),
	) {
		Column {
			Row(
				modifier =
				Modifier
					.fillMaxWidth()
					.padding(horizontal = 15.dp),
				verticalAlignment = Alignment.CenterVertically,
				horizontalArrangement = Arrangement.SpaceBetween,
			) {
				Row(
					verticalAlignment = Alignment.CenterVertically,
					horizontalArrangement = Arrangement.spacedBy(15.dp),
					modifier = Modifier.fillMaxWidth(13 / 20f),
				) {
					icon()
					Text(text, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelLarge)
				}
				rowButton()
			}
			if (expanded) {
				statistics?.getPeers()?.forEach {
					Row(
						modifier =
						Modifier
							.fillMaxWidth()
							.padding(end = 10.dp, bottom = 10.dp, start = 45.dp),
						verticalAlignment = Alignment.CenterVertically,
						horizontalArrangement = Arrangement.spacedBy(30.dp, Alignment.Start),
					) {
						val handshakeEpoch = statistics.peerStats(it)!!.latestHandshakeEpochMillis
						val peerId = it.toBase64().subSequence(0, 3).toString() + "***"
						val handshakeSec =
							NumberUtils.getSecondsBetweenTimestampAndNow(handshakeEpoch)
						val handshake =
							if (handshakeSec == null) stringResource(R.string.never) else "$handshakeSec " + stringResource(R.string.sec)
						val peerTx = statistics.peerStats(it)!!.txBytes
						val peerRx = statistics.peerStats(it)!!.rxBytes
						val peerTxMB = NumberUtils.bytesToMB(peerTx).toThreeDecimalPlaceString()
						val peerRxMB = NumberUtils.bytesToMB(peerRx).toThreeDecimalPlaceString()
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
		}
	}
}
