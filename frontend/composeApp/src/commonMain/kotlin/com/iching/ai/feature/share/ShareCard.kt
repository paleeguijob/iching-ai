package com.iching.ai.feature.share

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iching.ai.ui.components.CinnabarSeal
import com.iching.ai.ui.components.HexagramFigure

/** Sepia tones of an old oracle card. Fixed, not the selected theme, so shared cards look like one brand. */
private object CardInk {
    val paper = Color(0xFFF3E6C8)
    val paperEdge = Color(0xFFE2CB9A)
    val ink = Color(0xFF4E3220)
    val inkSoft = Color(0xFF7A5536)
    val wash = Color(0xFF8C6239)
    val frame = Color(0xFF9A7446)
    val cinnabar = Color(0xFF9C4221)
}

/**
 * The card itself: 360×640 dp, rendered at density 3 by [ShareCardSheet] to give 1080×1920 px.
 * Laid out like an oracle card: name top left, figure and number top right, the message, then a
 * landscape painted from the hexagram's two trigrams.
 */
@Composable
fun ShareCard(content: ShareCardContent, modifier: Modifier = Modifier) {
    val hexagram = content.hexagram
    Box(
        modifier
            .fillMaxSize()
            .background(Brush.radialGradient(listOf(CardInk.paper, CardInk.paper, CardInk.paperEdge), radius = 1400f))
            .padding(14.dp)
            .border(2.dp, CardInk.frame, RoundedCornerShape(10.dp))
            .padding(5.dp)
            .border(0.75.dp, CardInk.frame.copy(alpha = 0.7f), RoundedCornerShape(7.dp)),
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 18.dp, top = 18.dp)) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(hexagram.nameCn, style = MaterialTheme.typography.displayLarge, color = CardInk.ink)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            hexagram.shortNameTh,
                            style = MaterialTheme.typography.titleLarge,
                            color = CardInk.ink,
                            modifier = Modifier.padding(bottom = 12.dp),
                        )
                    }
                    Text(
                        hexagram.meaningTh,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = CardInk.cinnabar,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 10.dp)) {
                    HexagramFigure(hexagram.pattern, lineWidth = 52.dp, lineHeight = 6.dp, gap = 4.dp, color = CardInk.ink)
                    Text(
                        "${hexagram.id}",
                        style = MaterialTheme.typography.displayMedium.copy(fontSize = 40.sp, lineHeight = 48.sp),
                        color = CardInk.ink,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    content.body,
                    style = MaterialTheme.typography.bodyLarge,
                    color = CardInk.ink,
                    maxLines = if (content.judgment != null) 4 else 5,
                    overflow = TextOverflow.Ellipsis,
                )
                content.judgment?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = CardInk.inkSoft,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                TrigramScene(
                    hexagram,
                    SceneInk(ink = CardInk.ink, wash = CardInk.wash, paper = CardInk.paper, accent = CardInk.cinnabar),
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
                        color = CardInk.inkSoft,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .border(1.dp, CardInk.frame.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 3.dp),
                    )
                    Spacer(Modifier.height(8.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CinnabarSeal(text = "易", size = 28.dp, tilt = -6f, color = CardInk.cinnabar, glyphColor = CardInk.paper)
                    Spacer(Modifier.width(8.dp))
                    Text("อี้จิง", style = MaterialTheme.typography.titleMedium, color = CardInk.ink)
                }
            }
        }
    }
}
