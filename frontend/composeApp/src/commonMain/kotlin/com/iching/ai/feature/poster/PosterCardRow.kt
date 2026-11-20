package com.iching.ai.feature.poster

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.iching.ai.ui.strings.LocalStrings
import com.iching.ai.ui.theme.ZenColors
import com.iching.ai.ui.theme.ZenSpacing

/**
 * A promotion, kept quieter than the page around it: white card, thin edge, a small cinnabar seal tile. Tapping it
 * follows the poster's link; × closes this card for good.
 */
@Composable
fun PosterCardRow(card: PosterCard, onClick: () -> Unit, onDismiss: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(ZenColors.Card)
            .border(1.dp, ZenColors.current.outlineVariant, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(52.dp).clip(RoundedCornerShape(10.dp)).background(ZenColors.CinnabarRed),
            contentAlignment = Alignment.Center,
        ) {
            Text("易", style = MaterialTheme.typography.titleLarge, color = ZenColors.PaperCream)
        }
        Spacer(Modifier.width(ZenSpacing.md))
        Column(Modifier.weight(1f)) {
            Text(card.label, style = MaterialTheme.typography.labelSmall, color = ZenColors.CinnabarRed, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(card.title, style = MaterialTheme.typography.titleSmall, color = ZenColors.InkBlack, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(card.cta, style = MaterialTheme.typography.labelMedium, color = ZenColors.CinnabarRed)
        }
        IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.Top)) {
            Icon(Icons.Outlined.Close, contentDescription = LocalStrings.current.poster.closeCard, tint = ZenColors.InkFaint, modifier = Modifier.size(18.dp))
        }
    }
}

