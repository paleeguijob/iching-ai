package com.iching.ai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.iching.ai.ui.theme.ZenColors

/** Pill toggle: ink-filled when selected, a hairline outline when not. */
@Composable
fun ZenChip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = if (selected) ZenColors.PaperCream else ZenColors.InkSoft,
        modifier = modifier
            .clip(CircleShape)
            .background(if (selected) ZenColors.InkBlack else Color.Transparent)
            .border(1.dp, if (selected) ZenColors.InkBlack else ZenColors.current.outlineVariant, CircleShape)
            .semantics { this.selected = selected }
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
    )
}
