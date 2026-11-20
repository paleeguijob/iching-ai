package com.iching.ai.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.iching.ai.ui.components.ZenPrimaryButton
import com.iching.ai.ui.theme.ZenColors
import com.iching.ai.ui.theme.ZenSpacing
import com.iching.ai.ui.toThaiNumerals

/**
 * A plain note to the team. No stars and no "how do you feel" question here: asking that before the store's
 * rating sheet is what store policy forbids, so feedback stays its own path.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackSheet(
    ui: FeedbackUi,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden, enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)),
        containerColor = ZenColors.PaperCream,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = ZenSpacing.screen)
                .padding(bottom = ZenSpacing.lg),
        ) {
            Text("ส่งความเห็นถึงเรา", style = MaterialTheme.typography.titleMedium, color = ZenColors.InkBlack)
            Spacer(Modifier.height(ZenSpacing.xs))
            Text(
                "ข้อความถึงทีมผู้พัฒนาโดยตรง ไม่แสดงต่อสาธารณะ",
                style = MaterialTheme.typography.bodySmall,
                color = ZenColors.InkSoft,
            )
            Spacer(Modifier.height(ZenSpacing.lg))
            OutlinedTextField(
                value = ui.text,
                onValueChange = onTextChange,
                modifier = Modifier.fillMaxWidth().heightIn(min = 148.dp),
                enabled = !ui.sending,
                placeholder = { Text("ชอบตรงไหน อยากให้ปรับอะไร เล่าให้เราฟังได้เลย", color = ZenColors.InkFaint) },
                textStyle = MaterialTheme.typography.bodyMedium,
                shape = RoundedCornerShape(12.dp),
                isError = ui.error != null,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ZenColors.current.outline,
                    unfocusedBorderColor = ZenColors.current.outlineVariant,
                    focusedContainerColor = ZenColors.Card,
                    unfocusedContainerColor = ZenColors.Card,
                    disabledContainerColor = ZenColors.Card,
                    cursorColor = ZenColors.CinnabarRed,
                    errorBorderColor = ZenColors.CinnabarRed,
                    errorContainerColor = ZenColors.Card,
                    errorCursorColor = ZenColors.CinnabarRed,
                ),
            )
            Spacer(Modifier.height(ZenSpacing.sm))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Text(
                    ui.error.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = ZenColors.CinnabarRed,
                    modifier = Modifier.weight(1f).semantics { liveRegion = LiveRegionMode.Polite },
                )
                Text(
                    "${ui.text.length.toThaiNumerals()}/${FEEDBACK_MAX_CHARS.toThaiNumerals()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = ZenColors.InkFaint,
                    modifier = Modifier.padding(start = ZenSpacing.md),
                )
            }
            Spacer(Modifier.height(ZenSpacing.lg))
            ZenPrimaryButton(
                if (ui.sending) "กำลังส่ง…" else "ส่ง",
                onClick = onSend,
                enabled = ui.canSend,
                // The seal marks an action that is ready; on a greyed button it reads as a stray dot.
                showSeal = ui.canSend,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
