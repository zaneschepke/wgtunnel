package com.zaneschepke.wireguardautotunnel.ui.screens.support

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.zaneschepke.logcatter.model.LogMessage
import com.zaneschepke.wireguardautotunnel.R
import com.zaneschepke.wireguardautotunnel.ui.common.navigation.TopNavBar
import com.zaneschepke.wireguardautotunnel.ui.common.text.LogTypeLabel
import com.zaneschepke.wireguardautotunnel.viewmodel.LogsViewModel

@Composable
fun LogsScreen(viewModel: LogsViewModel = hiltViewModel()) {
	val logs = viewModel.logs

	val context = LocalContext.current
	val clipboardManager: ClipboardManager = LocalClipboardManager.current

	val lazyColumnListState = rememberLazyListState()
	var isAutoScrolling by remember { mutableStateOf(true) }
	var lastScrollPosition by remember { mutableIntStateOf(0) }

	LaunchedEffect(isAutoScrolling) {
		if (isAutoScrolling) {
			lazyColumnListState.animateScrollToItem(logs.size)
		}
	}

	LaunchedEffect(logs.size) {
		if (isAutoScrolling) {
			lazyColumnListState.animateScrollToItem(logs.size)
		}
	}

	LaunchedEffect(lazyColumnListState) {
		snapshotFlow { lazyColumnListState.firstVisibleItemIndex }
			.collect { currentScrollPosition ->
				if (currentScrollPosition < lastScrollPosition && isAutoScrolling) {
					isAutoScrolling = false
				}
				val visible = lazyColumnListState.layoutInfo.visibleItemsInfo
				if (visible.isNotEmpty()) {
					if (visible.last().index
						== lazyColumnListState.layoutInfo.totalItemsCount - 1 && !isAutoScrolling
					) {
						isAutoScrolling = true
					}
				}
				lastScrollPosition = currentScrollPosition
			}
	}

	Scaffold(
		topBar = {
			TopNavBar(stringResource(R.string.logs))
		},
		floatingActionButton = {
			FloatingActionButton(
				onClick = {
					viewModel.shareLogs(context)
				},
				shape = RoundedCornerShape(16.dp),
				containerColor = MaterialTheme.colorScheme.primary,
			) {
				val icon = Icons.Filled.Share
				Icon(
					imageVector = icon,
					contentDescription = icon.name,
					tint = MaterialTheme.colorScheme.onPrimary,
				)
			}
		},
	) {
		LazyColumn(
			horizontalAlignment = Alignment.CenterHorizontally,
			verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.Top),
			state = lazyColumnListState,
			modifier =
			Modifier
				.fillMaxSize()
				.padding(horizontal = 24.dp).padding(it),
		) {
			itemsIndexed(
				logs,
				key = { index, _ -> index },
				contentType = { _: Int, _: LogMessage -> null },
			) { _, it ->
				Row(
					horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.Start),
					verticalAlignment = Alignment.Top,
					modifier =
					Modifier
						.fillMaxSize()
						.clickable(
							interactionSource = remember { MutableInteractionSource() },
							indication = null,
							onClick = {
								clipboardManager.setText(
									annotatedString = AnnotatedString(it.toString()),
								)
							},
						),
				) {
					val fontSize = 10.sp
					Text(text = it.tag, modifier = Modifier.fillMaxSize(0.3f), fontSize = fontSize)
					LogTypeLabel(color = Color(it.level.color())) {
						Text(
							text = it.level.signifier,
							textAlign = TextAlign.Center,
							fontSize = fontSize,
						)
					}
					Text("${it.message} - ${it.time}", fontSize = fontSize)
				}
			}
		}
	}
}
