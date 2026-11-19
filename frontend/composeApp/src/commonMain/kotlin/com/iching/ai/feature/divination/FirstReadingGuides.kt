package com.iching.ai.feature.divination

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.iching.ai.domain.model.AppSettings
import com.iching.ai.domain.usecase.ObserveSettingsUseCase
import com.iching.ai.domain.usecase.UpdateSettingsUseCase
import com.iching.ai.ui.components.ZenCard
import com.iching.ai.ui.theme.ZenColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Which plain-language notes this reading shows. Read once when the screen opens, so a note does not
 * vanish halfway through the reading that marked it seen.
 */
@Stable
class FirstReadingGuides internal constructor(
    private val scope: CoroutineScope,
    private val update: UpdateSettingsUseCase,
) {
    var cast by mutableStateOf(false)
        internal set
    var reading by mutableStateOf(false)
        private set

    internal fun load(settings: AppSettings) {
        cast = !settings.castGuideSeen
        reading = !settings.readingGuideSeen
    }

    /** The first full cast is done; later casts go without the notes. Leaving mid-cast keeps them. */
    internal fun onCastDone() {
        scope.launch { update { it.copy(castGuideSeen = true) } }
    }

    /** Seen once it shows: it stays on this reading until dismissed, and never comes back. */
    fun onReadingGuideShown() {
        scope.launch { update { it.copy(readingGuideSeen = true) } }
    }

    fun dismissReading() {
        reading = false
    }
}

@Composable
internal fun rememberFirstReadingGuides(castDone: Boolean): FirstReadingGuides {
    val observe = koinInject<ObserveSettingsUseCase>()
    val update = koinInject<UpdateSettingsUseCase>()
    val scope = rememberCoroutineScope()
    val guides = remember { FirstReadingGuides(scope, update) }
    LaunchedEffect(Unit) { guides.load(observe().first()) }
    LaunchedEffect(castDone, guides.cast) {
        if (castDone && guides.cast) guides.onCastDone()
    }
    return guides
}

/** Under the cast on a newcomer's first reading: what the one or two hexagrams mean and where the full text is. */
@Composable
internal fun HowToReadNote(hasRelating: Boolean, onDismiss: () -> Unit) {
    ZenCard(contentPadding = PaddingValues(start = 16.dp, end = 8.dp, top = 14.dp, bottom = 4.dp)) {
        Column(Modifier.fillMaxWidth().padding(end = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("อ่านกวาอย่างไร", style = MaterialTheme.typography.titleSmall, color = ZenColors.InkBlack)
            Term("กวาหลัก", "ภาพของเรื่องนี้ในตอนนี้")
            if (hasRelating) {
                Term("กวาแปร", "ทิศที่เรื่องกำลังเปลี่ยนไป เกิดจากเส้นแปรที่ท่านโยนได้")
            } else {
                Text(
                    "ครั้งนี้ไม่มีเส้นแปร จึงมีเพียงกวาเดียว",
                    style = MaterialTheme.typography.bodySmall,
                    color = ZenColors.InkSoft,
                )
            }
            Text(
                "แตะที่กวาเพื่ออ่านตัวบทเต็มในคัมภีร์",
                style = MaterialTheme.typography.bodySmall,
                color = ZenColors.InkSoft,
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onDismiss) {
                Text("เข้าใจแล้ว", style = MaterialTheme.typography.labelLarge, color = ZenColors.BambooDeep)
            }
        }
    }
}

@Composable
private fun Term(term: String, meaning: String) {
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(color = ZenColors.BambooDeep)) { append(term) }
            append("  ")
            append(meaning)
        },
        style = MaterialTheme.typography.bodySmall,
        color = ZenColors.InkSoft,
    )
}
