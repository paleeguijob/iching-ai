package com.iching.ai.feature.divination

import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.background
import com.iching.ai.ui.theme.ZenSpacing
import com.iching.ai.ui.components.ZenTextButton
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.iching.ai.domain.model.CoinToss
import com.iching.ai.domain.model.LineType
import com.iching.ai.ui.components.AncientCoin
import com.iching.ai.ui.components.HexagramFigure
import com.iching.ai.ui.components.ZenPrimaryButton
import com.iching.ai.ui.theme.ZenColors
import com.iching.ai.ui.strings.LocalStrings

/**
 * The coin-toss stage shared by the reading and the year ritual: the hexagram building up line by line,
 * the three coins and the toss button. It shows whatever [tosses] it is given; the caller decides the faces.
 * [onTossRemaining] is null where there is no "toss until done" action. With [guide], the first cast also says
 * how to toss and what each line means in plain words.
 */
@Composable
internal fun TossStage(
    prompt: String,
    tosses: List<CoinToss>,
    currentToss: CoinToss?,
    tossKey: Int,
    isTossing: Boolean,
    awaitingCast: Boolean,
    canToss: Boolean,
    onToss: () -> Unit,
    onTossRemaining: (() -> Unit)? = null,
    guide: Boolean = false,
) {
    val strings = LocalStrings.current.divination
    val noRipple = remember { MutableInteractionSource() }
    Column(
        Modifier
            .fillMaxSize()
            .clickable(interactionSource = noRipple, indication = null, enabled = canToss, onClick = onToss)
            .navigationBarsPadding()
            .padding(horizontal = ZenSpacing.screen),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CastProgress(tosses.size)
        Spacer(Modifier.height(ZenSpacing.md))
        Text(
            prompt,
            style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
            color = ZenColors.InkSoft,
            textAlign = TextAlign.Center,
            maxLines = 3,
        )
        if (guide && tosses.isEmpty() && !awaitingCast) {
            Spacer(Modifier.height(ZenSpacing.sm))
            Text(
                strings.tossHint,
                style = MaterialTheme.typography.bodySmall,
                color = ZenColors.CinnabarRed,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.weight(0.6f))
        HexagramFigure(
            pattern = null,
            lines = tosses.map { it.line } + List(6 - tosses.size) { LineType.YOUNG_YANG },
            revealed = tosses.size,
            animateReveal = true,
            lineWidth = 150.dp,
            lineHeight = 14.dp,
            gap = 11.dp,
        )
        Spacer(Modifier.weight(0.5f))
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            val faces = currentToss?.faces
            repeat(3) { i ->
                AncientCoin(
                    face = faces?.get(i),
                    tossKey = tossKey,
                    // The middle coin lands a beat later, so the three don't settle as one.
                    delayMillis = i * 90 + if (i == 1) 80 else 0,
                    size = 78.dp,
                )
            }
        }
        Spacer(Modifier.height(18.dp))
        // Holds two lines while guiding so the coins do not jump when the plain-words note appears.
        Box(Modifier.heightIn(min = if (guide) 48.dp else 28.dp), contentAlignment = Alignment.Center) {
            val last = tosses.lastOrNull()
            if (awaitingCast) {
                Text(strings.settling, style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic), color = ZenColors.InkFaint)
            } else if (!isTossing && last != null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(strings.lineLabel(last.line), style = MaterialTheme.typography.bodyMedium, color = ZenColors.InkSoft)
                    if (guide) {
                        Text(
                            strings.lineGloss(last.line),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (last.line.isChanging) ZenColors.CinnabarRed else ZenColors.InkSoft,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.weight(0.4f))
        ZenPrimaryButton(
            text = if (tosses.isEmpty()) strings.toss else strings.tossNext,
            onClick = onToss,
            enabled = canToss,
            modifier = Modifier.fillMaxWidth(),
        )
        if (onTossRemaining != null) {
            ZenTextButton(strings.tossRest, onClick = onTossRemaining, enabled = canToss)
        }
        Spacer(Modifier.height(12.dp))
    }
}

/** "4 / 6" with a thin ink bar: how many of the six lines are cast. */
@Composable
private fun CastProgress(cast: Int) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            LocalStrings.current.divination.castProgress(cast),
            style = MaterialTheme.typography.labelMedium,
            color = ZenColors.InkFaint,
            modifier = Modifier.align(Alignment.End),
        )
        Spacer(Modifier.height(6.dp))
        Box(Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)).background(ZenColors.current.outlineVariant)) {
            Box(Modifier.fillMaxWidth(cast / 6f).fillMaxHeight().background(ZenColors.InkBlack))
        }
    }
}
