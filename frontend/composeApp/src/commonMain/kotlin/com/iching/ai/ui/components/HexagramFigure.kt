package com.iching.ai.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.iching.ai.domain.model.LineType
import com.iching.ai.ui.theme.ZenColors

/**
 * Draws a hexagram from a bottom-to-top pattern ("111111").
 * Optional [lines] marks changing lines (old yang / old yin) with a cinnabar ring around the line.
 * Positions beyond [revealed] are drawn as faint placeholders while a cast is in progress; lines
 * ink in as they are revealed only with [animateReveal]. Without it the figure is one Canvas instead
 * of a tree per line, which is what keeps lists of them cheap to scroll.
 */
@Composable
fun HexagramFigure(
    pattern: String?,
    modifier: Modifier = Modifier,
    lines: List<LineType>? = null,
    revealed: Int = 6,
    lineWidth: Dp = 96.dp,
    lineHeight: Dp = 12.dp,
    gap: Dp = 8.dp,
    color: Color = ZenColors.InkBlack,
    showChangeMarks: Boolean = true,
    animateReveal: Boolean = false,
) {
    if (!animateReveal) {
        StaticHexagram(pattern, lines, revealed, lineWidth, lineHeight, gap, color, showChangeMarks, modifier)
        return
    }
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(gap),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Drawn top (line 6) to bottom (line 1).
        for (position in 5 downTo 0) {
            val isRevealed = position < revealed
            val isYang = isYangAt(position, pattern, lines)
            val change = lines?.getOrNull(position)?.takeIf { it.isChanging && showChangeMarks }
            HexagramLine(
                isYang = isYang,
                visible = isRevealed,
                change = change,
                width = lineWidth,
                height = lineHeight,
                color = color,
            )
        }
    }
}

private fun isYangAt(position: Int, pattern: String?, lines: List<LineType>?): Boolean = when {
    lines != null && position < lines.size -> lines[position].isYang
    pattern != null && position < pattern.length -> pattern[position] == '1'
    else -> true
}

@Composable
private fun StaticHexagram(
    pattern: String?,
    lines: List<LineType>?,
    revealed: Int,
    lineWidth: Dp,
    lineHeight: Dp,
    gap: Dp,
    color: Color,
    showChangeMarks: Boolean,
    modifier: Modifier,
) {
    val markWidth = lineHeight * 0.5f
    Canvas(modifier.size(lineWidth + markWidth * 2, lineHeight * 6 + gap * 5)) {
        val w = lineWidth.toPx()
        val h = lineHeight.toPx()
        val markW = markWidth.toPx()
        for (position in 5 downTo 0) {
            val isRevealed = position < revealed
            val y = (5 - position) * (h + gap.toPx())
            if (isRevealed) {
                drawInkLine(isYangAt(position, pattern, lines), color, progress = 1f, left = markW, top = y, width = w, height = h)
                val change = lines?.getOrNull(position)?.takeIf { it.isChanging && showChangeMarks }
                if (change != null) drawChangeRing(left = markW, top = y, width = w, height = h)
            } else {
                drawPlaceholder(alpha = 1f, left = markW, top = y, width = w, height = h)
            }
        }
    }
}

@Composable
private fun HexagramLine(
    isYang: Boolean,
    visible: Boolean,
    change: LineType?,
    width: Dp,
    height: Dp,
    color: Color,
) {
    val progress by animateFloatAsState(if (visible) 1f else 0f, tween(700), label = "line")
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(height * 0.5f))
        Box(Modifier.width(width).height(height)) {
            Canvas(Modifier.fillMaxWidth().height(height)) {
                drawPlaceholder(alpha = 1f - progress)
                drawInkLine(isYang = isYang, color = color, progress = progress)
                if (change != null && visible) drawChangeRing(alpha = progress)
            }
        }
        Box(Modifier.width(height * 0.5f))
    }
}

/** Cinnabar ring around a changing line; drawn past the line's bounds into the gap around it. */
private fun DrawScope.drawChangeRing(
    alpha: Float = 1f,
    left: Float = 0f,
    top: Float = 0f,
    width: Float = size.width,
    height: Float = size.height,
) {
    if (alpha <= 0.01f) return
    val inset = height * 0.3f
    drawRoundRect(
        color = ZenColors.CinnabarSoft.copy(alpha = alpha),
        topLeft = Offset(left - inset, top - inset),
        size = Size(width + inset * 2, height + inset * 2),
        cornerRadius = CornerRadius(height * 0.65f),
        style = Stroke(width = height * 0.18f),
    )
}

private fun DrawScope.drawPlaceholder(
    alpha: Float,
    left: Float = 0f,
    top: Float = 0f,
    width: Float = size.width,
    height: Float = size.height,
) {
    if (alpha <= 0.01f) return
    // A dashed outline: the line that has not been cast yet.
    drawRoundRect(
        color = ZenColors.current.outline.copy(alpha = alpha),
        topLeft = Offset(left, top),
        size = Size(width, height),
        cornerRadius = CornerRadius(height / 2),
        style = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(height, height * 0.8f))),
    )
}

/** Brush-like line that grows from the centre outwards as [progress] goes 0 → 1. */
private fun DrawScope.drawInkLine(
    isYang: Boolean,
    color: Color,
    progress: Float,
    left: Float = 0f,
    top: Float = 0f,
    width: Float = size.width,
    height: Float = size.height,
) {
    if (progress <= 0f) return
    val radius = CornerRadius(height * 0.35f)
    val inked = color.copy(alpha = progress)
    if (isYang) {
        val w = width * progress
        drawRoundRect(inked, Offset(left + (width - w) / 2, top), Size(w, height), radius)
    } else {
        val gap = width * 0.16f
        val segment = (width - gap) / 2
        val w = segment * progress
        drawRoundRect(inked, Offset(left + segment - w, top), Size(w, height), radius)
        drawRoundRect(inked, Offset(left + segment + gap, top), Size(w, height), radius)
    }
}

/** A single yin or yang line, for compact lists. */
@Composable
fun LineGlyph(isYang: Boolean, modifier: Modifier = Modifier, color: Color = ZenColors.InkBlack) {
    Canvas(modifier) { drawInkLine(isYang = isYang, color = color, progress = 1f) }
}
