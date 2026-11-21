package com.iching.ai.ui.components

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import kotlin.math.ceil
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource
import kotlinx.coroutines.delay

/** New streamed words are laid out once the reveal is this close to the end of what is laid out... */
private const val TAKE_IN_AHEAD_CHARS = 120
/** ...but no more often than this, */
private val TAKE_IN_MIN_GAP = 150.milliseconds
/** and at least this often, so the height below the reveal keeps up. */
private val TAKE_IN_MAX_GAP = 1000.milliseconds

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
 * redraws; layout runs when a batch of streamed text is taken in or the reveal reaches a new line.
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
    // The text actually laid out. The oracle streams a few characters at a time, and laying out the whole
    // reading again for each of them stalls the main thread; while the reveal is well behind, new words wait
    // here and are taken in together.
    var laidOut by remember { mutableStateOf(text) }
    // Replaced rather than extended (asking again, or the trimmed final text): show the new text at once.
    val display = laidOut.takeIf { text.startsWith(it) } ?: text
    LaunchedEffect(animate) {
        if (!animate) {
            shown = latest.length
            return@LaunchedEffect
        }
        var lastTakeIn = TimeSource.Monotonic.markNow()
        while (true) {
            val next = latest
            if (!next.startsWith(laidOut)) {
                laidOut = next
                lastTakeIn = TimeSource.Monotonic.markNow()
            } else if (next.length > laidOut.length) {
                val since = lastTakeIn.elapsedNow()
                val nearEnd = laidOut.length - shown < TAKE_IN_AHEAD_CHARS
                if ((nearEnd && since >= TAKE_IN_MIN_GAP) || since >= TAKE_IN_MAX_GAP) {
                    laidOut = next
                    lastTakeIn = TimeSource.Monotonic.markNow()
                }
            }
            val target = laidOut
            if (shown < target.length) {
                shown = nextClusterEnd(target, shown).coerceAtMost(target.length)
                val ch = target[shown - 1]
                val behind = next.length - shown
                val pause = when {
                    behind > 600 -> 4L
                    behind > 250 -> charDelayMillis / 3
                    ch == '\n' -> charDelayMillis * 8
                    ch == ' ' -> charDelayMillis * 3
                    else -> charDelayMillis
                }
                delay(pause)
            } else {
                if (target.length == next.length) latestOnRevealed()
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
        display,
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
                val result = measurer.measure(display, style, constraints = Constraints(maxWidth = placeable.width))
                measured[0] = result
                val end = shown.coerceAtMost(result.layoutInput.text.length)
                val bottom = when {
                    end >= result.layoutInput.text.length -> placeable.height
                    else -> ceil(result.getLineBottom(result.getLineForOffset((end - 1).coerceAtLeast(0)))).toInt()
                }
                layout(placeable.width, bottom.coerceIn(constraints.minHeight, placeable.height)) { placeable.place(0, 0) }
            }
            // The laid-out text in its own layer: each revealed cluster only moves the clip above and replays it,
            // instead of drawing thousands of glyphs again.
            .graphicsLayer(),
        style = style,
    )
}
