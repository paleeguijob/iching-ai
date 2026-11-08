package com.iching.ai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.iching.ai.ui.theme.ZenColors

/**
 * Quiet Ink's raised paper card: card colour, soft shadow, and a hairline bottom edge so it still
 * separates from the page on dark themes where the shadow disappears. [accentTop] adds the cinnabar
 * rule used by the reading sheet.
 */
@Composable
fun ZenCard(
    modifier: Modifier = Modifier,
    accentTop: Boolean = false,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = if (accentTop) RoundedCornerShape(4.dp, 4.dp, 16.dp, 16.dp) else RoundedCornerShape(16.dp)
    val edge = ZenColors.current.outlineVariant
    Column(
        modifier
            .fillMaxWidth()
            .shadow(8.dp, shape, ambientColor = ZenColors.CardShadow, spotColor = ZenColors.CardShadow)
            .clip(shape)
            .background(ZenColors.Card)
            .drawWithContent {
                drawContent()
                val y = size.height - 0.5.dp.toPx()
                drawLine(edge, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
            }
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        if (accentTop) Box(Modifier.fillMaxWidth().height(3.dp).background(ZenColors.CinnabarRed))
        Column(Modifier.fillMaxWidth().padding(contentPadding), content = content)
    }
}

/** Hairline between rows inside a [ZenCard]. */
@Composable
fun ZenCardDivider(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(ZenColors.current.outlineVariant))
}
