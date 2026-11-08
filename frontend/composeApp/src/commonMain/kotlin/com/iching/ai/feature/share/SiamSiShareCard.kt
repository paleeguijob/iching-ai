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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iching.ai.ui.components.CinnabarSeal
import com.iching.ai.ui.theme.InkPaperPalette

/**
 * The Siam Si card, 360×640 dp like [ShareCard], always on the ink-and-paper palette whatever the theme:
 * set, number, level, poem, source. Built from [SiamSiShareContent], which has no question to show.
 */
@Composable
fun SiamSiShareCard(content: SiamSiShareContent, modifier: Modifier = Modifier) {
    val p = InkPaperPalette
    Box(
        modifier
            .fillMaxSize()
            .background(p.paperCream)
            .padding(14.dp)
            .border(2.dp, p.inkSoft, RoundedCornerShape(10.dp))
            .padding(5.dp)
            .border(0.75.dp, p.outline, RoundedCornerShape(7.dp)),
    ) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(content.setName, style = MaterialTheme.typography.titleMedium, color = p.inkBlack)
            Text("ใบที่", style = MaterialTheme.typography.labelMedium, color = p.inkSoft)
            Text(content.number, fontSize = 72.sp, lineHeight = 80.sp, color = p.cinnabarRed)
            content.labelOrig?.let { Text(it, fontFamily = FontFamily.Serif, fontSize = 18.sp, color = p.inkSoft) }
            Text(
                content.level,
                style = MaterialTheme.typography.labelLarge,
                color = p.inkBlack,
                modifier = Modifier.background(p.secondaryContainer, RoundedCornerShape(50)).padding(horizontal = 12.dp, vertical = 4.dp),
            )
            Spacer(Modifier.height(10.dp))
            content.poemOrig.forEach {
                Text(it, fontFamily = FontFamily.Serif, fontSize = 22.sp, color = p.inkBlack, textAlign = TextAlign.Center)
            }
            if (content.poemOrig.isNotEmpty()) Spacer(Modifier.height(6.dp))
            content.poemTh.forEach {
                Text(
                    it,
                    style = if (content.poemOrig.isEmpty()) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
                    color = if (content.poemOrig.isEmpty()) p.inkBlack else p.inkSoft,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.weight(1f))
            Text(content.source, style = MaterialTheme.typography.labelSmall, color = p.inkFaint, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Row(verticalAlignment = Alignment.CenterVertically) {
                CinnabarSeal(text = "籤", size = 28.dp, tilt = -6f, color = p.cinnabarRed, glyphColor = p.paperCream)
                Spacer(Modifier.width(8.dp))
                Text("อี้จิง AI · เซียมซี", style = MaterialTheme.typography.titleMedium, color = p.inkBlack)
            }
        }
    }
}
