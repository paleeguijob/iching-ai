package com.iching.ai.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iching.ai.ui.theme.ZenColors

/** Cream paper with faint ink-wash mountains along the bottom and an ensō in the corner. */
@Composable
fun PaperBackground(
    modifier: Modifier = Modifier,
    showMountains: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(ZenColors.PaperCream, Color(0xFFF7F4EC), ZenColors.PaperShade),
                )
            )
    ) {
        Canvas(Modifier.fillMaxSize()) {
            // Ensō — the Zen circle, left slightly open.
            val r = size.minDimension * 0.34f
            drawArc(
                color = ZenColors.InkBlack.copy(alpha = 0.035f),
                startAngle = -60f,
                sweepAngle = 320f,
                useCenter = false,
                topLeft = Offset(size.width - r * 1.35f, -r * 0.55f),
                size = Size(r * 2, r * 2),
                style = Stroke(width = r * 0.16f, cap = StrokeCap.Round),
            )
            if (showMountains) {
                drawMountains(0.80f, 0.055f, ZenColors.BambooGreen)
                drawMountains(0.87f, 0.075f, ZenColors.InkBlack)
            }
        }
        content()
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawMountains(baseline: Float, alpha: Float, color: Color) {
    val w = size.width
    val h = size.height
    val path = Path().apply {
        moveTo(0f, h)
        lineTo(0f, h * baseline)
        cubicTo(w * 0.12f, h * (baseline - 0.07f), w * 0.2f, h * (baseline - 0.11f), w * 0.3f, h * (baseline - 0.03f))
        cubicTo(w * 0.38f, h * (baseline + 0.02f), w * 0.46f, h * (baseline - 0.13f), w * 0.58f, h * (baseline - 0.06f))
        cubicTo(w * 0.7f, h * (baseline + 0.01f), w * 0.8f, h * (baseline - 0.09f), w * 0.9f, h * (baseline - 0.04f))
        cubicTo(w * 0.95f, h * (baseline - 0.02f), w, h * (baseline - 0.03f), w, h * (baseline - 0.02f))
        lineTo(w, h)
        close()
    }
    drawPath(path, Brush.verticalGradient(listOf(color.copy(alpha = alpha), color.copy(alpha = alpha * 0.2f))))
}

/** Square cinnabar seal stamp with Chinese characters arranged in columns, right to left. */
@Composable
fun CinnabarSeal(
    text: String = "易經",
    size: Dp = 56.dp,
    modifier: Modifier = Modifier,
    tilt: Float = -6f,
) {
    Box(
        modifier
            .rotate(tilt)
            .size(size)
            .background(ZenColors.CinnabarRed.copy(alpha = 0.92f), RoundedCornerShape(size * 0.12f))
            .border(size * 0.04f, ZenColors.PaperCream.copy(alpha = 0.5f), RoundedCornerShape(size * 0.1f))
            .padding(size * 0.05f),
        contentAlignment = Alignment.Center,
    ) {
        // One glyph fills the seal; two stack in a column; four sit in two columns read right to left.
        val columns = if (text.length > 2) text.chunked(2).reversed() else listOf(text)
        val glyphScale = when (text.length) {
            1 -> 0.52f
            2 -> 0.34f
            else -> 0.3f
        }
        val glyphStyle = TextStyle(
            color = ZenColors.PaperCream,
            fontSize = (size.value * glyphScale).sp,
            lineHeight = (size.value * glyphScale * 1.08f).sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
        )
        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            columns.forEach { column ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    column.forEach { ch -> Text(ch.toString(), style = glyphStyle) }
                }
            }
        }
    }
}

@Composable
fun ZenPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = ZenColors.InkBlack,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(54.dp),
        shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            contentColor = ZenColors.PaperCream,
            disabledContainerColor = color.copy(alpha = 0.25f),
            disabledContentColor = ZenColors.PaperCream,
        ),
        contentPadding = PaddingValues(horizontal = 32.dp),
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
fun ZenSecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, ZenColors.InkBlack.copy(alpha = 0.35f)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = ZenColors.InkBlack),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

/** Small caps-like label with a brush stroke on either side. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, color: Color = ZenColors.BambooDeep) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(18.dp).height(1.5.dp).background(color.copy(alpha = 0.6f)))
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, color = color)
        Spacer(Modifier.width(10.dp))
        Box(Modifier.width(18.dp).height(1.5.dp).background(color.copy(alpha = 0.6f)))
    }
}

/** Thin ink divider that fades at both ends. */
@Composable
fun InkDivider(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(
                Brush.horizontalGradient(
                    listOf(Color.Transparent, ZenColors.InkBlack.copy(alpha = 0.18f), Color.Transparent)
                )
            )
    )
}
