package com.iching.ai.ui.components

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import kotlin.math.ceil
import kotlinx.coroutines.delay

/** Thai vowels and tone marks that sit above/below the preceding consonant. */
private fun Char.isThaiCombining(): Boolean =
    this == 'ั' || this in 'ิ'..'ฺ' || this in '็'..'๎'

/** End index of the grapheme starting at [start], so marks never appear on their own. */
internal fun nextClusterEnd(text: String, start: Int): Int {
    var end = start + 1
    while (end < text.length && (text[end].isThaiCombining() || text[end].isLowSurrogate())) end++
    return end
}

/**
 * Where to cut between [line] and the next one. Not the line's bottom: Thai marks stacked above a
 * consonant rise past the top of their line box, and would show before their line is reached.
 */
private fun TextLayoutResult.inkBottom(line: Int): Float =
    getLineBaseline(line) + (getLineBottom(line) - getLineTop(line)) * 0.28f

/**
 * Reveals [text] one character cluster at a time. [text] may keep growing while
 * the oracle streams; the reveal catches up faster when it falls far behind.
 * When [animate] is false the whole text is shown immediately.
 *
 * The whole text is laid out once and uncovered by clipping, because laying out thousands of Thai
 * characters again for every new one is too slow for mid-range phones. Each revealed cluster only
 * redraws; layout runs when more text arrives or the reveal reaches a new line.
 */
@Composable
fun TypewriterText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    animate: Boolean = true,
    charDelayMillis: Long = 32,
    onRevealed: () -> Unit = {},
) {
    // Saveable: coming back from another screen must not type the reading out again.
    var shown by rememberSaveable { mutableIntStateOf(if (animate) 0 else text.length) }
    val latest by rememberUpdatedState(text)
    val latestOnRevealed by rememberUpdatedState(onRevealed)
    LaunchedEffect(animate) {
        if (!animate) {
            shown = latest.length
            return@LaunchedEffect
        }
        while (true) {
            val target = latest
            if (shown < target.length) {
                shown = nextClusterEnd(target, shown).coerceAtMost(target.length)
                val ch = target[shown - 1]
                val behind = target.length - shown
                val pause = when {
                    behind > 600 -> 4L
                    behind > 250 -> charDelayMillis / 3
                    ch == '\n' -> charDelayMillis * 8
                    ch == ' ' -> charDelayMillis * 3
                    else -> charDelayMillis
                }
                delay(pause)
            } else {
                latestOnRevealed()
                delay(60)
            }
        }
    }
    if (!animate) {
        Text(text, modifier = modifier, style = style)
        return
    }
    // Measured inside the same layout pass that sizes the text, so the height and the reveal always come from the
    // same lines. (A layout handed back through onTextLayout can be a pass behind: the text then reported the
    // height of an earlier chunk while drawing more, and ran over whatever sat below it.)
    val measurer = rememberTextMeasurer()
    val measured = remember { arrayOfNulls<TextLayoutResult>(1) }
    Text(
        text,
        modifier = modifier
            // Outside the layout below, so this scope has the reported size and the clip holds back anything past it.
            .drawWithContent {
                val result = measured[0] ?: return@drawWithContent
                val end = shown.coerceAtMost(result.layoutInput.text.length)
                if (end == 0) return@drawWithContent
                clipRect(bottom = size.height) {
                    if (end == result.layoutInput.text.length) {
                        this@drawWithContent.drawContent()
                        return@clipRect
                    }
                    val line = result.getLineForOffset(end - 1)
                    val top = if (line == 0) 0f else result.inkBottom(line - 1)
                    if (line > 0) clipRect(bottom = top) { this@drawWithContent.drawContent() }
                    val right = if (end >= result.getLineEnd(line)) size.width else result.getHorizontalPosition(end, usePrimaryDirection = true)
                    clipRect(top = top, bottom = result.inkBottom(line), right = right) { this@drawWithContent.drawContent() }
                }
            }
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                // The measurer caches by text and width, so a new cluster on the same line costs a lookup, not a layout.
                val result = measurer.measure(text, style, constraints = Constraints(maxWidth = placeable.width))
                measured[0] = result
                val end = shown.coerceAtMost(result.layoutInput.text.length)
                val bottom = when {
                    end >= result.layoutInput.text.length -> placeable.height
                    else -> ceil(result.getLineBottom(result.getLineForOffset((end - 1).coerceAtLeast(0)))).toInt()
                }
                layout(placeable.width, bottom.coerceIn(constraints.minHeight, placeable.height)) { placeable.place(0, 0) }
            },
        style = style,
    )
}
