package com.iching.ai.feature.siamsi

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.iching.ai.domain.siamsi.SiamSiSet
import com.iching.ai.ui.components.ZenCard
import com.iching.ai.ui.components.ZenCardDivider
import com.iching.ai.ui.theme.ZenColors
import com.iching.ai.ui.theme.ZenSpacing
import com.iching.ai.ui.strings.LocalStrings
import com.iching.ai.ui.strings.SiamSiStrings

/** The prefix the set files put on every book's name; the screens show the name without it. Thai data key, never displayed as is. */
private const val SET_NAME_PREFIX = "เซียมซี"

/** The set's name without the leading "Siam Si" word. */
internal fun SiamSiSet.nameWithoutPrefix(): String = name.removePrefix(SET_NAME_PREFIX)

/** The set row on Home: name and number of slips. */
fun SiamSiSet.shortLabel(strings: SiamSiStrings): String = strings.setRow(nameWithoutPrefix(), count)

/** Picks the set to draw from. Only the sets the server lists and the device holds appear. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SiamSiSetSheet(sets: List<SiamSiSet>, selectedId: String?, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    val strings = LocalStrings.current.siamsi
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden, enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)),
        containerColor = ZenColors.PaperCream,
    ) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = ZenSpacing.screen).padding(bottom = 20.dp)) {
            Text(strings.setSheetTitle, style = MaterialTheme.typography.titleMedium, color = ZenColors.InkBlack)
            Text(strings.setSheetSubtitle, style = MaterialTheme.typography.bodySmall, color = ZenColors.InkSoft)
            Spacer(Modifier.height(ZenSpacing.md))
            ZenCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                sets.forEachIndexed { i, set ->
                    if (i > 0) ZenCardDivider()
                    SetRow(set, set.id == selectedId) {
                        onSelect(set.id)
                        onDismiss()
                    }
                }
            }
        }
    }
}

@Composable
private fun SetRow(set: SiamSiSet, selected: Boolean, onClick: () -> Unit) {
    val strings = LocalStrings.current.siamsi
    Row(
        Modifier
            .fillMaxWidth()
            .semantics { this.selected = selected }
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(set.nameWithoutPrefix(), style = MaterialTheme.typography.titleSmall, color = ZenColors.InkBlack)
            Text(
                strings.setSource(set.sourceTitle, set.count),
                style = MaterialTheme.typography.bodySmall,
                color = ZenColors.InkSoft,
            )
        }
        Spacer(Modifier.width(12.dp))
        Box(
            Modifier
                .size(18.dp)
                .clip(CircleShape)
                .border(1.5.dp, if (selected) ZenColors.CinnabarRed else ZenColors.current.outlineVariant, CircleShape)
                .padding(4.dp)
                .clip(CircleShape)
                .background(if (selected) ZenColors.CinnabarRed else androidx.compose.ui.graphics.Color.Transparent),
        )
    }
}
