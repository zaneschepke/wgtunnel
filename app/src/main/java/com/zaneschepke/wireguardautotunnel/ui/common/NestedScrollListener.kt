package com.zaneschepke.wireguardautotunnel.ui.common

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource

class NestedScrollListener( val onUp: () -> Unit, val onDown: () -> Unit) : NestedScrollConnection {
	override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
		if (available.y < -1) onDown()
		if (available.y > 1) onUp()
		return Offset.Zero
	}
}
