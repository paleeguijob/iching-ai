package com.iching.ai.feature.siamsi

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iching.ai.ui.components.SectionLabel
import com.iching.ai.ui.theme.ZenColors
import com.iching.ai.ui.theme.ZenPalette
import com.iching.ai.ui.toThaiNumerals

private const val KNOCK_MILLIS = 180

private val TUBE_W = 118.dp
private val TUBE_H = 220.dp

/** Sticks fanned in the tube: left edge (fraction of the tube), height (dp) and lean (degrees). */
private val STICKS = listOf(
    Triple(0.10f, 80f, -9f), Triple(0.21f, 96f, -5f), Triple(0.32f, 88f, -2f), Triple(0.44f, 104f, 1f),
    Triple(0.55f, 91f, 4f), Triple(0.66f, 99f, 7f), Triple(0.77f, 78f, 10f),
)

/**
 * The cylinder marked 籤 with its sticks, on a soft shadow. Holding it shakes it ([onHoldChange]); while
 * [shaking] it rocks and the phone taps in time with the sticks.
 */
@Composable
fun SiamSiCylinder(shaking: Boolean, onHoldChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val palette = ZenColors.current
    val haptics = LocalHapticFeedback.current
    val tilt = remember { Animatable(0f) }
    LaunchedEffect(shaking) {
        if (!shaking) {
            tilt.animateTo(0f, tween(260, easing = FastOutSlowInEasing))
            return@LaunchedEffect
        }
        var beat = 0
        while (true) {
            tilt.animateTo(if (beat % 2 == 0) 7f else -7f, tween(KNOCK_MILLIS, easing = FastOutSlowInEasing))
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            beat++
        }
    }
    Box(
        modifier
            .fillMaxWidth()
            .height(TUBE_H + 150.dp)
            .semantics { contentDescription = "กระบอกเซียมซี กดค้างเพื่อเขย่า" }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown()
                    onHoldChange(true)
                    waitForUpOrCancellation()
                    onHoldChange(false)
                }
            },
        contentAlignment = Alignment.BottomCenter,
    ) {
        ShadowRing(Modifier.padding(bottom = 4.dp))
        Box(
            Modifier
                .padding(bottom = 22.dp)
                .size(width = TUBE_W + 40.dp, height = TUBE_H + 120.dp)
                .graphicsLayer {
                    rotationZ = tilt.value
                    transformOrigin = TransformOrigin(0.5f, 0.85f)
                },
            contentAlignment = Alignment.BottomCenter,
        ) {
            Canvas(Modifier.matchParentSize()) {
                val left = (size.width - TUBE_W.toPx()) / 2
                val rim = size.height - TUBE_H.toPx()
                drawSticks(palette, left, rim, sway = tilt.value)
                drawTube(palette, Offset(left, rim), Size(TUBE_W.toPx(), TUBE_H.toPx()))
            }
            TubeLabel(Modifier.padding(bottom = TUBE_H * 0.52f))
        }
    }
}

/** The fallen stick: its number large in cinnabar above the tipped cylinder, with the stick lying in front. */
@Composable
fun FallenStage(number: Int, modifier: Modifier = Modifier) {
    val palette = ZenColors.current
    val drop = remember(number) { Animatable(0f) }
    LaunchedEffect(number) { drop.animateTo(1f, tween(520, easing = FastOutSlowInEasing)) }
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        SectionLabel("ไม้ที่ได้")
        Text(
            number.toThaiNumerals(),
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 56.sp, lineHeight = 64.sp, fontWeight = FontWeight.Bold),
            color = palette.cinnabarRed,
            modifier = Modifier.semantics { contentDescription = "ได้ไม้หมายเลข $number" },
        )
        Text("ไม้เซียมซีที่ ${number.toThaiNumerals()}", style = MaterialTheme.typography.titleSmall, color = palette.inkBlack)
        Box(Modifier.fillMaxWidth().height(TUBE_H + 40.dp), contentAlignment = Alignment.BottomCenter) {
            ShadowRing(Modifier.padding(bottom = 30.dp))
            Box(
                Modifier
                    .padding(bottom = 58.dp)
                    .size(TUBE_W, TUBE_H)
                    .graphicsLayer {
                        rotationZ = -14f
                        scaleX = 0.82f
                        scaleY = 0.82f
                        transformOrigin = TransformOrigin(0.5f, 1f)
                    },
                contentAlignment = Alignment.BottomCenter,
            ) {
                Canvas(Modifier.matchParentSize()) { drawTube(palette, Offset.Zero, size) }
                TubeLabel(Modifier.padding(bottom = TUBE_H * 0.52f))
            }
            Canvas(
                Modifier
                    .padding(bottom = 46.dp)
                    .offset(x = 26.dp)
                    .size(width = 196.dp, height = 12.dp)
                    .graphicsLayer {
                        rotationZ = -12f
                        alpha = drop.value
                        translationY = (1f - drop.value) * -80f
                    },
            ) {
                val r = CornerRadius(size.height / 2)
                drawRoundRect(Brush.horizontalGradient(listOf(palette.stickDeep, palette.stickLight)), cornerRadius = r)
                drawRoundRect(palette.cinnabarRed, Offset(size.width - 28.dp.toPx(), 0f), Size(28.dp.toPx(), size.height), r)
            }
        }
    }
}

@Composable
private fun TubeLabel(modifier: Modifier) {
    Text(
        "籤",
        color = ZenColors.current.woodLabel.copy(alpha = 0.9f),
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        textAlign = TextAlign.Center,
        modifier = modifier,
    )
}

@Composable
private fun ShadowRing(modifier: Modifier) {
    val shade = ZenColors.current.cardShadow
    Canvas(modifier.size(width = 200.dp, height = 30.dp)) {
        // A flattened radial fade, as wide as the oval: the gradient is drawn round and squashed vertically.
        val strong = shade.copy(alpha = (shade.alpha * 4f).coerceAtMost(0.5f))
        scale(scaleX = 1f, scaleY = size.height / size.width, pivot = center) {
            drawCircle(Brush.radialGradient(0f to strong, 0.7f to Color.Transparent, center = center, radius = size.width / 2), radius = size.width / 2, center = center)
        }
    }
}

private fun DrawScope.drawTube(p: ZenPalette, at: Offset, tube: Size) {
    drawRoundRect(
        Brush.horizontalGradient(
            0f to p.woodDeep, 0.3f to p.wood, 0.5f to p.woodLight, 0.7f to p.wood, 1f to p.woodDeep,
            startX = at.x, endX = at.x + tube.width,
        ),
        at, tube, CornerRadius(14.dp.toPx()),
    )
    val band = 9.dp.toPx()
    listOf(0.22f, 0.80f).forEach { f ->
        drawRoundRect(p.woodDeep, Offset(at.x - 2.dp.toPx(), at.y + tube.height * f), Size(tube.width + 4.dp.toPx(), band), CornerRadius(4.dp.toPx()))
    }
}

/** Each stick leans from its foot inside the tube; [sway] jostles them a little while shaking. */
private fun DrawScope.drawSticks(p: ZenPalette, tubeLeft: Float, rim: Float, sway: Float) {
    val w = 8.dp.toPx()
    val foot = rim + 40.dp.toPx()
    STICKS.forEachIndexed { i, (x, h, lean) ->
        val left = tubeLeft + TUBE_W.toPx() * x
        val height = h.dp.toPx() + 40.dp.toPx()
        val jostle = if (i % 2 == 0) sway * 0.6f else -sway * 0.4f
        rotate(lean + jostle, pivot = Offset(left + w / 2, foot)) {
            val top = foot - height
            drawRoundRect(Brush.verticalGradient(listOf(p.stickLight, p.stickDeep), startY = top, endY = foot), Offset(left, top), Size(w, height), CornerRadius(w / 2))
            drawRoundRect(p.cinnabarRed.copy(alpha = 0.85f), Offset(left, top), Size(w, 13.dp.toPx()), CornerRadius(w / 2))
        }
    }
}
