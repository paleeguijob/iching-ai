package com.iching.ai.feature.siamsi

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.iching.ai.domain.siamsi.PuaResult
import com.iching.ai.ui.theme.ZenColors


/** What each pua result looks like and says: which faces land up (flat = ventral side up). */
private fun PuaResult.faces(): Pair<Boolean, Boolean> = when (this) {
    PuaResult.SAK -> false to true
    PuaResult.LAUGH -> true to true
    PuaResult.NO -> false to false
}

fun PuaResult.labelTh(): String = when (this) {
    PuaResult.SAK -> "ศักดิ์ — คว่ำหนึ่ง หงายหนึ่ง ฟ้ายืนยันใบนี้"
    PuaResult.LAUGH -> "หัวเราะ — หงายทั้งคู่ เขย่าใหม่อีกครั้ง"
    PuaResult.NO -> "ไม่ — คว่ำทั้งคู่ เขย่าใหม่อีกครั้ง"
}

/**
 * The pair of crescent pua blocks. A new [result] replays a short toss before the blocks settle on their faces;
 * null shows them resting. Tapping the blocks tosses them ([onToss]), as does the screen's main button.
 */
@Composable
fun PuaBlocks(result: PuaResult?, onToss: () -> Unit, modifier: Modifier = Modifier) {
    val palette = ZenColors.current
    val hop = remember { Animatable(0f) }
    LaunchedEffect(result) {
        if (result == null) return@LaunchedEffect
        hop.snapTo(1f)
        hop.animateTo(0f, tween(650))
    }
    val (leftUp, rightUp) = result?.faces() ?: (false to true)
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            modifier = Modifier
                .clickable(onClickLabel = "เสี่ยงไม้ปัว", onClick = onToss)
                .semantics { contentDescription = result?.labelTh() ?: "ไม้ปัวสองอัน" },
        ) {
            listOf(leftUp to false, rightUp to true).forEach { (flatUp, mirror) ->
                Canvas(
                    Modifier
                        .size(width = 76.dp, height = 44.dp)
                        .graphicsLayer {
                            translationY = -60f * hop.value
                            rotationZ = (if (mirror) 1 else -1) * 200f * hop.value
                            scaleX = if (mirror) -1f else 1f
                        },
                ) {
                    // A crescent: the round back, or the flat face when it lands face up.
                    if (flatUp) {
                        drawArc(palette.cinnabarRed, 180f, 180f, true, Offset.Zero, Size(size.width, size.height * 2))
                        drawLine(palette.cinnabarSoft, Offset(0f, size.height), Offset(size.width, size.height), 3.dp.toPx())
                    } else {
                        drawArc(palette.cinnabarSoft, 180f, 180f, true, Offset.Zero, Size(size.width, size.height * 2))
                        drawArc(palette.cinnabarRed, 200f, 140f, false, Offset(6f, 6f), Size(size.width - 12f, size.height * 2 - 12f), style = Stroke(4.dp.toPx()))
                    }
                }
            }
        }
        if (result != null) {
            Text(
                result.labelTh(),
                style = MaterialTheme.typography.bodyMedium,
                color = if (result == PuaResult.SAK) palette.cinnabarRed else palette.inkSoft,
            )
        }
    }
}
