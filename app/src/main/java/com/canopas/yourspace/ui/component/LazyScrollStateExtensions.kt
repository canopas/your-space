package com.canopas.yourspace.ui.component

import androidx.compose.foundation.lazy.LazyListState

internal fun LazyListState.reachedBottom(buffer: Int = 2): Boolean {
    val lastVisibleItem = this.layoutInfo.visibleItemsInfo.lastOrNull()
    return lastVisibleItem?.index != 0 && lastVisibleItem?.index == this.layoutInfo.totalItemsCount - buffer
}
