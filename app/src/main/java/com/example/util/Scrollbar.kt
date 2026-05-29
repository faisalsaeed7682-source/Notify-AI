package com.example.util

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp

fun Modifier.simpleVerticalScrollbar(
    state: LazyListState,
    width: androidx.compose.ui.unit.Dp = 4.dp
): Modifier = composed {
    val targetAlpha = if (state.isScrollInProgress) 1f else 0f
    val duration = if (state.isScrollInProgress) 150 else 500

    val alpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(durationMillis = duration),
        label = "scrollbar_alpha"
    )

    val color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f * alpha)

    drawWithContent {
        drawContent()

        val firstVisibleElementIndex = state.layoutInfo.visibleItemsInfo.firstOrNull()?.index
        if (alpha > 0.0f && firstVisibleElementIndex != null && state.layoutInfo.totalItemsCount > 0) {
            val elementHeight = this.size.height / state.layoutInfo.totalItemsCount
            val scrollbarOffsetY = firstVisibleElementIndex * elementHeight
            val scrollbarHeight = state.layoutInfo.visibleItemsInfo.size * elementHeight

            drawRect(
                color = color,
                topLeft = Offset(this.size.width - width.toPx(), scrollbarOffsetY),
                size = Size(width.toPx(), scrollbarHeight),
                alpha = 1.0f
            )
        }
    }
}

fun Modifier.simpleVerticalScrollbar(
    state: ScrollState,
    width: androidx.compose.ui.unit.Dp = 4.dp
): Modifier = composed {
    val targetAlpha = if (state.isScrollInProgress) 1f else 0f
    val duration = if (state.isScrollInProgress) 150 else 500

    val alpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(durationMillis = duration),
        label = "scrollbar_alpha"
    )

    val color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f * alpha)

    drawWithContent {
        drawContent()

        if (alpha > 0.0f && state.maxValue > 0) {
            val visibleHeight = this.size.height
            val totalHeight = visibleHeight + state.maxValue
            val scrollbarHeight = (visibleHeight / totalHeight) * visibleHeight
            val scrollbarOffsetY = (state.value.toFloat() / state.maxValue.toFloat()) * (visibleHeight - scrollbarHeight)

            drawRect(
                color = color,
                topLeft = Offset(this.size.width - width.toPx(), scrollbarOffsetY),
                size = Size(width.toPx(), scrollbarHeight),
                alpha = 1.0f
            )
        }
    }
}
