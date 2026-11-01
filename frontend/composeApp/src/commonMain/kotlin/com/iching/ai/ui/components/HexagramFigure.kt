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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.iching.ai.domain.model.LineType
import com.iching.ai.ui.theme.ZenColors

/**
 * Draws a hexagram from a bottom-to-top pattern ("111111").
 * Optional [lines] marks changing lines with a cinnabar dot (old yang) or cross (old yin).
 * Positions beyond [revealed] are drawn as faint placeholders while a cast is in progress.
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
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(gap),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Drawn top (line 6) to bottom (line 1).
        for (position in 5 downTo 0) {
            val isRevealed = position < revealed
            val isYang = when {
                lines != null && position < lines.size -> lines[position].isYang
                pattern != null && position < pattern.length -> pattern[position] == '1'
                else -> true
            }
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
        Box(Modifier.width(height * 1.6f))
        Box(Modifier.width(width).height(height)) {
            Canvas(Modifier.fillMaxWidth().height(height)) {
                drawPlaceholder(isYang = isYang, alpha = 1f - progress)
                drawInkLine(isYang = isYang, color = color, progress = progress)
            }
        }
        if (change != null && visible) {
            Canvas(Modifier.width(height * 1.6f).height(height)) {
                val r = size.height * 0.42f
                val c = Offset(size.width * 0.65f, size.height / 2)
                if (change == LineType.OLD_YANG) {
                    drawCircle(ZenColors.CinnabarRed, r, c, style = Stroke(width = size.height * 0.16f))
                } else {
                    val s = size.height * 0.16f
                    drawLine(ZenColors.CinnabarRed, c + Offset(-r, -r), c + Offset(r, r), s)
                    drawLine(ZenColors.CinnabarRed, c + Offset(-r, r), c + Offset(r, -r), s)
                }
            }
        } else {
            // Keep both sides symmetric whether or not a mark is shown.
            Box(Modifier.width(height * 1.6f))
        }
    }
}

private fun DrawScope.drawPlaceholder(isYang: Boolean, alpha: Float) {
    if (alpha <= 0.01f) return
    drawRoundRect(
        color = ZenColors.InkFaint.copy(alpha = 0.22f * alpha),
        size = size,
        cornerRadius = CornerRadius(size.height / 2),
    )
}

/** Brush-like line that grows from the centre outwards as [progress] goes 0 → 1. */
private fun DrawScope.drawInkLine(isYang: Boolean, color: Color, progress: Float) {
    if (progress <= 0f) return
    val h = size.height
    val radius = CornerRadius(h * 0.35f)
    val inked = color.copy(alpha = progress)
    if (isYang) {
        val w = size.width * progress
        drawRoundRect(inked, Offset((size.width - w) / 2, 0f), Size(w, h), radius)
    } else {
        val gap = size.width * 0.16f
        val segment = (size.width - gap) / 2
        val w = segment * progress
        drawRoundRect(inked, Offset(segment - w, 0f), Size(w, h), radius)
        drawRoundRect(inked, Offset(segment + gap, 0f), Size(w, h), radius)
    }
}

/** A single yin or yang line, for compact lists. */
@Composable
fun LineGlyph(isYang: Boolean, modifier: Modifier = Modifier, color: Color = ZenColors.InkBlack) {
    Canvas(modifier) { drawInkLine(isYang = isYang, color = color, progress = 1f) }
}
