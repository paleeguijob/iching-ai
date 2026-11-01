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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.iching.ai.ui.components.CinnabarSeal
import com.iching.ai.ui.components.HexagramFigure
import com.iching.ai.ui.theme.InkPaperPalette

/**
 * The card itself: 360×640 dp, rendered at density 3 by [ShareCardSheet] to give 1080×1920 px.
 * Always the original palette (not the selected theme), so shared cards look like one brand.
 */
@Composable
fun ShareCard(content: ShareCardContent, modifier: Modifier = Modifier) {
    val palette = InkPaperPalette
    val hexagram = content.hexagram
    // The judgment and summary together need more room than a single message.
    val compact = content.judgment != null
    Box(
        modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(palette.scrollEdge, palette.scroll, palette.scrollEdge))),
    ) {
        Roller(Modifier.align(Alignment.TopCenter))
        Roller(Modifier.align(Alignment.BottomCenter))
        Column(
            Modifier.fillMaxSize().padding(horizontal = 30.dp, vertical = 44.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("กวาที่ ${hexagram.id}", style = MaterialTheme.typography.labelMedium, color = palette.bambooDeep)
            Spacer(Modifier.height(16.dp))
            if (compact) {
                HexagramFigure(hexagram.pattern, lineWidth = 64.dp, lineHeight = 7.dp, gap = 5.dp, color = palette.inkBlack)
            } else {
                HexagramFigure(hexagram.pattern, lineWidth = 88.dp, lineHeight = 10.dp, gap = 7.dp, color = palette.inkBlack)
            }
            Spacer(Modifier.height(if (compact) 8.dp else 14.dp))
            Text(
                hexagram.nameCn,
                style = if (compact) MaterialTheme.typography.displayMedium else MaterialTheme.typography.displayLarge,
                color = palette.inkBlack,
            )
            Text(hexagram.shortNameTh, style = MaterialTheme.typography.titleMedium, color = palette.inkSoft)
            Text(
                hexagram.meaningTh,
                style = MaterialTheme.typography.bodySmall,
                color = palette.bambooDeep,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(if (compact) 8.dp else 16.dp))
            content.judgment?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.inkSoft,
                    textAlign = TextAlign.Center,
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(10.dp))
            }
            Column(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.Center) {
                Text(
                    content.body,
                    style = MaterialTheme.typography.bodyLarge,
                    color = palette.inkBlack,
                    textAlign = TextAlign.Center,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = if (compact) 4 else 8,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (content.aiLabel) {
                Text(
                    "เนื้อหาสร้างโดย AI · เพื่อการสะท้อนคิด",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.inkSoft,
                    modifier = Modifier
                        .border(1.dp, palette.outline, RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 3.dp),
                )
                Spacer(Modifier.height(12.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                CinnabarSeal(text = "易", size = 34.dp, tilt = -6f, color = palette.cinnabarRed, glyphColor = palette.paperCream)
                Spacer(Modifier.width(10.dp))
                Text("อี้จิง", style = MaterialTheme.typography.headlineSmall, color = palette.inkBlack)
            }
        }
    }
}

@Composable
private fun Roller(modifier: Modifier) {
    val palette = InkPaperPalette
    Box(
        modifier
            .fillMaxWidth()
            .height(20.dp)
            .background(Brush.verticalGradient(listOf(palette.scrollRollerLight, palette.scrollWood, palette.scrollRollerDark))),
    )
}
