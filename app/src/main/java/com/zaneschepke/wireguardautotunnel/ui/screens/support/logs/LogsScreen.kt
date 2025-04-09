package com.zaneschepke.wireguardautotunnel.ui.screens.support.logs

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zaneschepke.wireguardautotunnel.ui.screens.support.logs.components.LogList
import com.zaneschepke.wireguardautotunnel.ui.screens.support.logs.components.LogsBottomSheet
import com.zaneschepke.wireguardautotunnel.ui.state.AppViewState
import com.zaneschepke.wireguardautotunnel.viewmodel.AppViewModel

@Composable
fun LogsScreen(appViewState: AppViewState, viewModel: AppViewModel) {
    val logs by viewModel.logs.collectAsStateWithLifecycle()

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
                if (
                    visible.isNotEmpty() &&
                        visible.last().index ==
                            lazyColumnListState.layoutInfo.totalItemsCount - 1 &&
                        !isAutoScrolling
                ) {
                    isAutoScrolling = true
                }
                lastScrollPosition = currentScrollPosition
            }
    }

    if (appViewState.showBottomSheet) {
        LogsBottomSheet(viewModel)
    }

    LogList(
        logs = logs,
        lazyColumnListState = lazyColumnListState,
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
    )
}
