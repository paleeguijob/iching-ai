package com.iching.ai.feature.divination

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
import com.iching.ai.ui.toThaiNumerals

/**
 * The coin-toss stage shared by the reading and the year ritual: the hexagram building up line by line,
 * the three coins and the toss button. It shows whatever [tosses] it is given; the caller decides the faces.
 * [onTossRemaining] is null where there is no "toss until done" action.
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
) {
    val noRipple = remember { MutableInteractionSource() }
    Column(
        Modifier
            .fillMaxSize()
            .clickable(interactionSource = noRipple, indication = null, enabled = canToss, onClick = onToss)
            .navigationBarsPadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(4.dp))
        Text(
            prompt,
            style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
            color = ZenColors.InkSoft,
            textAlign = TextAlign.Center,
            maxLines = 3,
        )
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
        Spacer(Modifier.height(14.dp))
        Text(
            "เส้นที่ ${(tosses.size + if (tosses.size < 6) 1 else 0).coerceAtMost(6).toThaiNumerals()} จาก ๖",
            style = MaterialTheme.typography.labelLarge,
            color = ZenColors.BambooDeep,
        )
        Spacer(Modifier.weight(0.5f))
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            val faces = currentToss?.faces
            repeat(3) { i ->
                AncientCoin(
                    face = faces?.get(i),
                    tossKey = tossKey,
                    delayMillis = i * 90,
                    size = 78.dp,
                )
            }
        }
        Spacer(Modifier.height(18.dp))
        Box(Modifier.height(28.dp), contentAlignment = Alignment.Center) {
            val last = tosses.lastOrNull()
            if (awaitingCast) {
                Text("กำลังตั้งจิต...", style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic), color = ZenColors.InkFaint)
            } else if (!isTossing && last != null) {
                Text(lineLabel(last.line), style = MaterialTheme.typography.bodyMedium, color = ZenColors.InkSoft)
            }
        }
        Spacer(Modifier.weight(0.4f))
        ZenPrimaryButton(
            text = if (tosses.isEmpty()) "โยนเหรียญ" else "โยนครั้งต่อไป",
            onClick = onToss,
            enabled = canToss,
            modifier = Modifier.fillMaxWidth(),
        )
        if (onTossRemaining != null) {
            TextButton(onClick = onTossRemaining, enabled = canToss) {
                Text("โยนต่อเนื่องจนครบ", color = ZenColors.InkSoft, style = MaterialTheme.typography.labelMedium)
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

private fun lineLabel(line: LineType): String = when (line) {
    LineType.OLD_YIN -> "หยินแก่ (๖) — เส้นแปร"
    LineType.YOUNG_YANG -> "หยางอ่อน (๗)"
    LineType.YOUNG_YIN -> "หยินอ่อน (๘)"
    LineType.OLD_YANG -> "หยางแก่ (๙) — เส้นแปร"
}
