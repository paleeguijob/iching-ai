package com.iching.ai.ui.components

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
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
 * Reveals [text] one character cluster at a time. [text] may keep growing while
 * the oracle streams; the reveal catches up faster when it falls far behind.
 * When [animate] is false the whole text is shown immediately.
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
    var shown by remember { mutableIntStateOf(if (animate) 0 else text.length) }
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
    val visible = if (animate) text.take(shown.coerceAtMost(text.length)) else text
    Text(visible, modifier = modifier, style = style)
}
