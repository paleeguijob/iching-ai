package com.iching.ai.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iching.ai.domain.model.CoinFace
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.cos

private val Bronze = Color(0xFFB08D57)
private val BronzeDark = Color(0xFF7A5C33)
private val BronzeLight = Color(0xFFD9BE8A)

/**
 * A square-holed bronze coin. [face] is the side to land on; changing [tossKey]
 * replays the flip animation (several spins, a little hop, then settle).
 */
@Composable
fun AncientCoin(
    face: CoinFace?,
    tossKey: Int,
    modifier: Modifier = Modifier,
    size: Dp = 84.dp,
    delayMillis: Int = 0,
) {
    val rotation = remember { Animatable(0f) }
    val lift = remember { Animatable(0f) }
    LaunchedEffect(tossKey) {
        if (tossKey == 0 || face == null) return@LaunchedEffect
        val spins = 5 + (tossKey + delayMillis) % 3
        // Ends on 0° (heads) or 180° (tails) after several full turns.
        val target = spins * 360f + if (face == CoinFace.TAILS) 180f else 0f
        rotation.snapTo(0f)
        launch {
            lift.snapTo(0f)
            lift.animateTo(1f, tween(520 + delayMillis / 2, delayMillis, FastOutSlowInEasing))
            lift.animateTo(0f, tween(520, easing = FastOutSlowInEasing))
        }
        rotation.animateTo(target, tween(1100, delayMillis, FastOutSlowInEasing))
    }
    val angle = rotation.value % 360f
    val showingTails = face != null && angle in 90f..270f
    Box(
        modifier
            .size(size)
            .graphicsLayer {
                rotationX = rotation.value
                cameraDistance = 14f * density
                translationY = -lift.value * size.toPx() * 0.9f
                val s = 1f + lift.value * 0.18f
                scaleX = s
                scaleY = s
            },
        contentAlignment = Alignment.Center,
    ) {
        val shade = 0.65f + 0.35f * abs(cos(rotation.value * kotlin.math.PI.toFloat() / 180f))
        Canvas(Modifier.fillMaxSize()) {
            val r = this.size.minDimension / 2
            val c = Offset(this.size.width / 2, this.size.height / 2)
            drawCircle(
                Brush.radialGradient(
                    listOf(BronzeLight, Bronze, BronzeDark),
                    center = c - Offset(r * 0.3f, r * 0.3f),
                    radius = r * 1.5f,
                ),
                r, c, alpha = shade,
            )
            drawCircle(BronzeDark.copy(alpha = 0.7f), r * 0.93f, c, style = Stroke(r * 0.05f))
            drawCircle(BronzeDark.copy(alpha = 0.35f), r * 0.82f, c, style = Stroke(r * 0.025f))
            val hole = r * 0.42f
            drawRoundRect(
                Color(0xFFF3EEE2),
                topLeft = c - Offset(hole / 2, hole / 2),
                size = Size(hole, hole),
                cornerRadius = CornerRadius(hole * 0.08f),
            )
            drawRoundRect(
                BronzeDark.copy(alpha = 0.8f),
                topLeft = c - Offset(hole / 2 + r * 0.06f, hole / 2 + r * 0.06f),
                size = Size(hole + r * 0.12f, hole + r * 0.12f),
                cornerRadius = CornerRadius(hole * 0.1f),
                style = Stroke(r * 0.04f),
            )
            if (showingTails) {
                // Yin side: four crescent marks instead of characters.
                listOf(Offset(0f, -1f), Offset(1f, 0f), Offset(0f, 1f), Offset(-1f, 0f)).forEach { d ->
                    drawCircle(BronzeDark.copy(alpha = 0.55f), r * 0.09f, c + d * (r * 0.6f))
                }
            }
        }
        if (!showingTails) {
            // Yang side: 通寶 (top/bottom) and 乾坤 (right/left) around the hole.
            val glyph = (size.value * 0.2f).sp
            Box(Modifier.fillMaxSize()) {
                CoinGlyph("通", Alignment.TopCenter, glyph, Modifier.padding(top = size * 0.12f))
                CoinGlyph("寶", Alignment.BottomCenter, glyph, Modifier.padding(bottom = size * 0.12f))
                CoinGlyph("乾", Alignment.CenterEnd, glyph, Modifier.padding(end = size * 0.13f))
                CoinGlyph("坤", Alignment.CenterStart, glyph, Modifier.padding(start = size * 0.13f))
            }
        }
    }
}

@Composable
private fun BoxScope.CoinGlyph(text: String, alignment: Alignment, fontSize: TextUnit, inset: Modifier) {
    Text(
        text,
        modifier = Modifier.align(alignment).then(inset),
        color = BronzeDark,
        fontSize = fontSize,
        lineHeight = fontSize,
        fontWeight = FontWeight.Bold,
    )
}
