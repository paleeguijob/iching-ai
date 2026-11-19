package com.iching.ai.feature.daily

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.iching.ai.feature.home.CampaignLine
import com.iching.ai.feature.home.StreakWeek
import com.iching.ai.ui.components.ZenCard
import com.iching.ai.ui.theme.ZenColors
import com.iching.ai.ui.theme.ZenSpacing

/**
 * Seven numbered circles of the current run: ticked once counted, today ringed while it is still waiting, a gift
 * where a theme unlocks. Opening this page is what counts the day, so here it only reports.
 */
@Composable
internal fun StreakWeekRow(week: StreakWeek, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth()) {
            week.days.forEach { day ->
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(if (day.done) ZenColors.CinnabarSoft else Color.Transparent)
                            .border(
                                width = if (day.isToday && !day.done) 1.5.dp else 1.dp,
                                color = when {
                                    day.done -> ZenColors.CinnabarSoft
                                    day.isToday -> ZenColors.CinnabarRed
                                    else -> ZenColors.current.outline
                                },
                                shape = CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        when {
                            day.done -> Icon(Icons.Outlined.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            day.milestone -> Icon(
                                Icons.Outlined.CardGiftcard,
                                contentDescription = null,
                                tint = ZenColors.CinnabarRed,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        day.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (day.isToday) ZenColors.InkBlack else ZenColors.InkFaint,
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            week.caption,
            style = MaterialTheme.typography.bodySmall,
            color = ZenColors.InkSoft,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** The year hexagram as one card row near the end of the page; a red dot marks a year not cast yet. */
@Composable
internal fun CampaignLineRow(line: CampaignLine, onClick: () -> Unit) {
    ZenCard(onClick = onClick, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (line.pending) {
                Box(Modifier.size(6.dp).clip(CircleShape).background(ZenColors.CinnabarRed))
                Spacer(Modifier.width(ZenSpacing.sm))
            }
            Text(
                line.text,
                style = MaterialTheme.typography.bodyMedium,
                color = ZenColors.InkBlack,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                "${line.action} ›",
                style = MaterialTheme.typography.labelLarge,
                color = ZenColors.CinnabarRed,
                modifier = Modifier.padding(start = ZenSpacing.sm),
            )
        }
    }
}
