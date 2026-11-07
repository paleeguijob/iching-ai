package com.iching.ai.feature.share

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iching.ai.ui.components.CinnabarSeal
import com.iching.ai.ui.components.HexagramFigure

/** Colours of one card style. Fixed, not the selected theme, so shared cards look like one brand. */
private class CardPalette(
    val paper: Color,
    val paperEdge: Color,
    val ink: Color,
    val inkSoft: Color,
    val wash: Color,
    val frame: Color,
    val cinnabar: Color,
    val sealGlyph: Color,
) {
    companion object {
        /** Sepia tones of an old oracle card. */
        val Classic = CardPalette(
            paper = Color(0xFFF3E6C8), paperEdge = Color(0xFFE2CB9A), ink = Color(0xFF4E3220),
            inkSoft = Color(0xFF7A5536), wash = Color(0xFF8C6239), frame = Color(0xFF9A7446),
            cinnabar = Color(0xFF9C4221), sealGlyph = Color(0xFFF3E6C8),
        )

        /** Deep cinnabar ground with gold ink, for the new-year campaign. */
        val NewYear = CardPalette(
            paper = Color(0xFF8E1B16), paperEdge = Color(0xFF5C0E0F), ink = Color(0xFFF2D488),
            inkSoft = Color(0xFFD9B56A), wash = Color(0xFFC79A45), frame = Color(0xFFD9B56A),
            cinnabar = Color(0xFFF2D488), sealGlyph = Color(0xFF8E1B16),
        )
    }
}

/**
 * The card itself: 360×640 dp, rendered at density 3 by [ShareCardSheet] to give 1080×1920 px.
 * Laid out like an oracle card: name top left, figure and number top right, the message, then a
 * landscape painted from the hexagram's two trigrams.
 */
@Composable
fun ShareCard(content: ShareCardContent, modifier: Modifier = Modifier) {
    val hexagram = content.hexagram
    val newYear = content.style == CardStyle.NEW_YEAR
    val palette = if (newYear) CardPalette.NewYear else CardPalette.Classic
    Box(
        modifier
            .fillMaxSize()
            .background(Brush.radialGradient(listOf(palette.paper, palette.paper, palette.paperEdge), radius = 1400f))
            .padding(14.dp)
            .border(2.dp, palette.frame, RoundedCornerShape(10.dp))
            .padding(5.dp)
            .border(0.75.dp, palette.frame.copy(alpha = 0.7f), RoundedCornerShape(7.dp)),
    ) {
        if (newYear) AuspiciousCloudBorder(palette.frame, Modifier.fillMaxSize())
        Column(Modifier.fillMaxSize().then(if (newYear) Modifier.padding(horizontal = 6.dp, vertical = 8.dp) else Modifier)) {
            content.yearLabel?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = palette.ink,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
                )
            }
            Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 18.dp, top = if (content.yearLabel != null) 10.dp else 18.dp)) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(hexagram.nameCn, style = MaterialTheme.typography.displayLarge, color = palette.ink)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            hexagram.shortNameTh,
                            style = MaterialTheme.typography.titleLarge,
                            color = palette.ink,
                            modifier = Modifier.padding(bottom = 12.dp),
                        )
                    }
                    Text(
                        hexagram.meaningTh,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = palette.cinnabar,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 10.dp)) {
                    HexagramFigure(hexagram.pattern, lineWidth = 52.dp, lineHeight = 6.dp, gap = 4.dp, color = palette.ink)
                    Text(
                        "${hexagram.id}",
                        style = MaterialTheme.typography.displayMedium.copy(fontSize = 40.sp, lineHeight = 48.sp),
                        color = palette.ink,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    content.body,
                    style = MaterialTheme.typography.bodyLarge,
                    color = palette.ink,
                    maxLines = if (content.judgment != null) 4 else 5,
                    overflow = TextOverflow.Ellipsis,
                )
                content.judgment?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.inkSoft,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                TrigramScene(
                    hexagram,
                    SceneInk(ink = palette.ink, wash = palette.wash, paper = palette.paper, accent = palette.cinnabar),
                    Modifier.fillMaxSize().padding(horizontal = 6.dp),
                )
            }
            Column(
                Modifier.fillMaxWidth().padding(bottom = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (content.aiLabel) {
                    Text(
                        "เนื้อหาสร้างโดย AI · เพื่อการสะท้อนคิด",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.inkSoft,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .border(1.dp, palette.frame.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 3.dp),
                    )
                    Spacer(Modifier.height(8.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CinnabarSeal(text = "易", size = 28.dp, tilt = -6f, color = palette.cinnabar, glyphColor = palette.sealGlyph)
                    Spacer(Modifier.width(8.dp))
                    Text("อี้จิง", style = MaterialTheme.typography.titleMedium, color = palette.ink)
                }
            }
        }
    }
}

/**
 * Auspicious clouds (ruyi) along the inside of the card edge: a scalloped run of curled arcs on each
 * side and a larger cloud in each corner. Paths only, so there is nothing to license.
 */
@Composable
private fun AuspiciousCloudBorder(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val stroke = Stroke(width = 1.2.dp.toPx(), cap = StrokeCap.Round)
        val r = 7.dp.toPx()
        val inset = 5.dp.toPx()
        val w = size.width
        val h = size.height
        val countX = ((w - 2 * inset - 4 * r) / (2 * r)).toInt()
        val countY = ((h - 2 * inset - 4 * r) / (2 * r)).toInt()
        // One curl: a half circle with a smaller one tucked inside, opening away from the edge.
        fun curl(center: Offset, startAngle: Float) {
            drawArc(color, startAngle, 180f, false, Offset(center.x - r, center.y - r), Size(2 * r, 2 * r), style = stroke)
            val s = r * 0.5f
            drawArc(color, startAngle, 180f, false, Offset(center.x - s, center.y - s), Size(2 * s, 2 * s), style = stroke)
        }
        for (i in 0 until countX) {
            val x = inset + 2 * r + r + i * (w - 2 * inset - 4 * r) / countX.coerceAtLeast(1)
            curl(Offset(x, inset + r), 0f)
            curl(Offset(x, h - inset - r), 180f)
        }
        for (i in 0 until countY) {
            val y = inset + 2 * r + r + i * (h - 2 * inset - 4 * r) / countY.coerceAtLeast(1)
            curl(Offset(inset + r, y), 90f)
            curl(Offset(w - inset - r, y), 270f)
        }
        // Corner clouds: three nested quarter turns.
        val corner = 2.4f * r
        listOf(
            Triple(Offset(inset, inset), 1f, 1f),
            Triple(Offset(w - inset, inset), -1f, 1f),
            Triple(Offset(inset, h - inset), 1f, -1f),
            Triple(Offset(w - inset, h - inset), -1f, -1f),
        ).forEach { (origin, sx, sy) ->
            val path = Path().apply {
                moveTo(origin.x + sx * corner, origin.y)
                cubicTo(
                    origin.x + sx * corner, origin.y + sy * corner * 0.6f,
                    origin.x + sx * corner * 0.6f, origin.y + sy * corner,
                    origin.x, origin.y + sy * corner,
                )
                cubicTo(
                    origin.x + sx * corner * 0.35f, origin.y + sy * corner * 0.65f,
                    origin.x + sx * corner * 0.65f, origin.y + sy * corner * 0.35f,
                    origin.x + sx * corner, origin.y,
                )
            }
            drawPath(path, color, style = stroke)
            drawCircle(color, radius = r * 0.35f, center = Offset(origin.x + sx * r * 0.9f, origin.y + sy * r * 0.9f))
        }
    }
}
