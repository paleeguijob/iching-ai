package com.iching.ai.feature.siamsi

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.iching.ai.domain.siamsi.MAX_PUA_ROUNDS
import com.iching.ai.domain.siamsi.PuaResult
import com.iching.ai.ui.components.ZenCard
import com.iching.ai.ui.theme.ZenColors
import com.iching.ai.ui.theme.ZenSpacing

/** Which faces land up for each result: true = หงาย (flat face up). */
private fun PuaResult.faces(): Pair<Boolean, Boolean> = when (this) {
    PuaResult.SAK -> true to false
    PuaResult.LAUGH -> true to true
    PuaResult.NO -> false to false
}

fun PuaResult.labelTh(): String = when (this) {
    PuaResult.SAK -> "ศักดิ์ — คว่ำหนึ่ง หงายหนึ่ง ฟ้ายืนยันใบนี้"
    PuaResult.LAUGH -> "หัวเราะ — หงายทั้งคู่ เขย่าใหม่อีกครั้ง"
    PuaResult.NO -> "ไม่ — คว่ำทั้งคู่ เขย่าใหม่อีกครั้ง"
}

/** The result card's headline and the two lines under it. */
fun puaHeadline(result: PuaResult, autoConfirmed: Boolean): Pair<String, String> = when {
    autoConfirmed -> "ครบสามรอบ · รับใบนี้" to "ไม้ปัวยังไม่ได้ศักดิ์ แต่ครบสามรอบแล้ว\nฟ้าให้ใบสุดท้ายนี้แก่คุณ"
    result == PuaResult.SAK -> "ศักดิ์ · ใช่ใบนี้" to "ไม้ปัวคว่ำหนึ่ง หงายหนึ่ง\nฟ้ารับคำอธิษฐานของคุณแล้ว"
    result == PuaResult.LAUGH -> "หัวเราะ · ยังไม่ใช่" to "ไม้ปัวหงายทั้งคู่\nเขย่าใหม่อีกครั้ง ไม่เสียสิทธิ์"
    else -> "ไม่ · ไม่ใช่ใบนี้" to "ไม้ปัวคว่ำทั้งคู่\nเขย่าใหม่อีกครั้ง ไม่เสียสิทธิ์"
}

/**
 * The two crescent blocks with หงาย / คว่ำ under each. A new [result] replays a short toss before they settle;
 * null shows them resting, unlabelled. Tapping them tosses ([onToss], null once tossed).
 */
@Composable
fun PuaBlocks(result: PuaResult?, onToss: (() -> Unit)?, modifier: Modifier = Modifier) {
    val hop = remember { Animatable(0f) }
    LaunchedEffect(result) {
        if (result == null) return@LaunchedEffect
        hop.snapTo(1f)
        hop.animateTo(0f, tween(700, easing = FastOutSlowInEasing))
    }
    val (leftUp, rightUp) = result?.faces() ?: (true to false)
    Row(
        modifier
            .then(
                if (onToss != null) {
                    Modifier.clickable(remember { MutableInteractionSource() }, indication = null, onClickLabel = "โยนไม้ปัว", onClick = onToss)
                } else {
                    Modifier
                },
            )
            .semantics { contentDescription = result?.labelTh() ?: "ไม้ปัวสองอัน แตะเพื่อโยน" },
        horizontalArrangement = Arrangement.spacedBy(34.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Block(up = leftUp, spin = -1, hop = hop.value, label = if (result != null) leftUp else null)
        Block(up = rightUp, spin = 1, hop = hop.value, label = if (result != null) rightUp else null)
    }
}

/** Block size from the design (74×40 at 300 wide), scaled up for a phone. */
private val BLOCK_W = 96.dp
private val BLOCK_H = 52.dp

@Composable
private fun Block(up: Boolean, spin: Int, hop: Float, label: Boolean?) {
    val p = ZenColors.current
    // As in the design: a rounded slab, domed on top when it lands หงาย and flipped when คว่ำ,
    // the หงาย one leaning left and the คว่ำ one right.
    val shape = if (up) {
        RoundedCornerShape(topStart = 42.dp, topEnd = 42.dp, bottomEnd = 10.dp, bottomStart = 10.dp)
    } else {
        RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp, bottomEnd = 42.dp, bottomStart = 42.dp)
    }
    val lean = if (up) -6f else 9f
    val colors = if (up) listOf(p.puaLight, p.puaDeep) else listOf(p.puaShade, p.puaDark)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(BLOCK_W, BLOCK_H)
                .graphicsLayer {
                    rotationZ = lean + spin * 320f * hop
                    translationY = -110f * hop
                }
                .shadow(10.dp, shape, ambientColor = p.cardShadow, spotColor = p.cardShadow)
                .background(Brush.verticalGradient(colors), shape),
        )
        Spacer(Modifier.height(14.dp))
        Text(
            when (label) {
                true -> "หงาย"
                false -> "คว่ำ"
                null -> " "
            },
            style = MaterialTheme.typography.labelSmall,
            color = ZenColors.InkSoft,
        )
    }
}

/** The outcome of the last toss, with one dot per round: cinnabar = ศักดิ์, grey = not, empty = still to come. */
@Composable
fun PuaResultCard(results: List<PuaResult>, autoConfirmed: Boolean, modifier: Modifier = Modifier) {
    val last = results.lastOrNull() ?: return
    val (headline, detail) = puaHeadline(last, autoConfirmed)
    ZenCard(modifier.fillMaxWidth()) {
        Text(
            headline,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = ZenColors.CinnabarRed,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(ZenSpacing.xs))
        Text(detail, style = MaterialTheme.typography.bodySmall, color = ZenColors.InkSoft, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(ZenSpacing.md))
        RoundDots(results, Modifier.align(Alignment.CenterHorizontally))
    }
}

@Composable
fun RoundDots(results: List<PuaResult>, modifier: Modifier = Modifier) {
    val p = ZenColors.current
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(MAX_PUA_ROUNDS) { i ->
            val color = when (results.getOrNull(i)) {
                PuaResult.SAK -> p.cinnabarRed
                null -> p.outlineVariant
                else -> p.inkFaint
            }
            Box(Modifier.size(9.dp).clip(CircleShape).background(color))
        }
    }
}
