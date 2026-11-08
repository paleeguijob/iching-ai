package com.iching.ai.feature.siamsi

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iching.ai.ui.theme.ZenColors
import com.iching.ai.ui.toThaiNumerals
import kotlin.math.sin

private val Bamboo = Color(0xFFB98B4E)
private val BambooDark = Color(0xFF7D5A2E)
private val Stick = Color(0xFFE2C48E)
private const val KNOCK_MILLIS = 180L

/**
 * The bamboo cylinder marked 籤. Holding it down shakes it ([onHoldChange]); while [shaking] the sticks jostle
 * and the phone taps in time with them. Once [fallenNumber] is set, one stick lies below with its number in cinnabar.
 */
@Composable
fun SiamSiCylinder(shaking: Boolean, fallenNumber: Int?, onHoldChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val palette = ZenColors.current
    val haptics = LocalHapticFeedback.current
    val tilt = remember { Animatable(0f) }
    LaunchedEffect(shaking) {
        if (!shaking) {
            tilt.animateTo(0f, tween(220, easing = FastOutSlowInEasing))
            return@LaunchedEffect
        }
        var beat = 0
        while (true) {
            tilt.animateTo(if (beat % 2 == 0) 9f else -9f, tween(KNOCK_MILLIS.toInt(), easing = FastOutSlowInEasing))
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            beat++
        }
    }
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            Modifier
                .size(width = 132.dp, height = 220.dp)
                .semantics { contentDescription = "กระบอกเซียมซี กดค้างเพื่อเขย่า" }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown()
                        onHoldChange(true)
                        waitForUpOrCancellation()
                        onHoldChange(false)
                    }
                }
                .graphicsLayer {
                    rotationZ = tilt.value
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f)
                },
            contentAlignment = Alignment.BottomCenter,
        ) {
            Canvas(Modifier.size(width = 132.dp, height = 220.dp)) {
                val tubeW = size.width * 0.72f
                val tubeH = size.height * 0.62f
                val left = (size.width - tubeW) / 2
                val top = size.height - tubeH
                // Sticks fanned above the rim.
                for (i in 0 until 9) {
                    val x = left + tubeW * (0.12f + i * 0.095f)
                    val rise = size.height * (0.12f + 0.06f * sin(i * 1.7f + tilt.value / 6f).let { it * it })
                    drawRoundRect(Stick, Offset(x, top - rise), Size(5.dp.toPx(), tubeH * 0.5f + rise), CornerRadius(3f))
                    drawRoundRect(palette.cinnabarRed.copy(alpha = 0.85f), Offset(x, top - rise), Size(5.dp.toPx(), 7.dp.toPx()), CornerRadius(3f))
                }
                drawRoundRect(
                    Brush.horizontalGradient(listOf(BambooDark, Bamboo, BambooDark), startX = left, endX = left + tubeW),
                    Offset(left, top), Size(tubeW, tubeH), CornerRadius(14.dp.toPx()),
                )
                // Bamboo nodes.
                listOf(0.18f, 0.82f).forEach { f ->
                    drawRect(BambooDark, Offset(left, top + tubeH * f), Size(tubeW, 2.dp.toPx()))
                }
            }
            Text(
                "籤",
                color = palette.paperCream,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 62.dp),
            )
        }
        AnimatedVisibility(fallenNumber != null, enter = fadeIn(tween(300)) + slideInVertically(tween(420)) { -it }) {
            FallenStick(fallenNumber ?: 0)
        }
    }
}

@Composable
private fun FallenStick(number: Int) {
    val palette = ZenColors.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .width(200.dp)
                .height(16.dp)
                .graphicsLayer { rotationZ = -6f }
                .background(Stick, RoundedCornerShape(8.dp)),
        )
        Text(
            number.toThaiNumerals(),
            color = palette.cinnabarRed,
            fontSize = 56.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics { contentDescription = "ได้ไม้หมายเลข $number" },
        )
    }
}
