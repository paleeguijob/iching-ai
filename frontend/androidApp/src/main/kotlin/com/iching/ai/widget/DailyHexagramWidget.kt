package com.iching.ai.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.color.ColorProvider
import com.iching.ai.MainActivity
import com.iching.ai.domain.model.AppLanguage
import com.iching.ai.domain.oracle.bangkokEpochDay
import com.iching.ai.domain.repository.HexagramRepository
import com.iching.ai.domain.usecase.ObserveSettingsUseCase
import com.iching.ai.ui.strings.AppLanguageHolder
import com.iching.ai.ui.strings.stringsFor
import com.iching.ai.ui.theme.InkPaperPalette
import com.iching.ai.ui.theme.paletteFor
import com.iching.ai.domain.model.AppTheme
import org.koin.mp.KoinPlatform
import kotlin.time.Clock

private val Small = DpSize(110.dp, 110.dp)
private val Wide = DpSize(250.dp, 110.dp)

/** What the widget draws: today's hexagram, or only a line of text when it can't be read. */
private data class WidgetState(val content: WidgetContent?, val emptyText: String)

/** The daily hexagram on the home screen: 2x2 shows the symbol and name, 4x2 adds the day's message. */
class DailyHexagramWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(setOf(Small, Wide))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val state = loadState()
        provideContent { WidgetBody(context, state) }
    }

    /**
     * The widget and the worker run without App(), but the process always starts through IChingApplication, which
     * resolves the AppLanguageHolder (wiring the hexagram language). Getting it here too keeps that explicit.
     */
    private suspend fun loadState(): WidgetState {
        val koin = KoinPlatform.getKoin()
        val holder = koin.get<AppLanguageHolder>()
        val lang = runCatchingCancellable { awaitLanguage(koin.get<ObserveSettingsUseCase>()(), holder.language) }
            .getOrDefault(holder.language.value)
        val empty = stringsFor(lang).widget.empty
        val content = runCatchingCancellable {
            widgetContentFor(bangkokEpochDay(Clock.System.now().toEpochMilliseconds()), koin.get<HexagramRepository>())
        }.getOrNull()
        return WidgetState(content, empty)
    }
}

private val paper = ColorProvider(day = InkPaperPalette.paperCream, night = paletteFor(AppTheme.INK_NIGHT).paperCream)
private val ink = ColorProvider(day = InkPaperPalette.inkBlack, night = paletteFor(AppTheme.INK_NIGHT).inkBlack)
private val inkSoft = ColorProvider(day = InkPaperPalette.inkSoft, night = paletteFor(AppTheme.INK_NIGHT).inkSoft)
private val accent = ColorProvider(day = InkPaperPalette.cinnabarRed, night = paletteFor(AppTheme.INK_NIGHT).cinnabarRed)

/** Same extra as the daily push, so MainActivity opens the same screen; "source" tells the two apart for analytics. */
fun widgetLaunchIntent(context: Context): Intent =
    Intent(context, MainActivity::class.java)
        .putExtra("type", "daily")
        .putExtra("source", "widget")

@Composable
private fun WidgetBody(context: Context, state: WidgetState) {
    val wide = LocalSize.current.width >= Wide.width
    val tap = actionStartActivity(widgetLaunchIntent(context))
    val content = state.content
    Column(
        modifier = GlanceModifier.fillMaxSize().background(paper).cornerRadius(20.dp).padding(12.dp).clickable(tap),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (content == null) {
            Text(state.emptyText, style = TextStyle(color = ink, fontSize = 14.sp, textAlign = TextAlign.Center), maxLines = 3)
        } else {
            Text(content.symbol, style = TextStyle(color = accent, fontSize = if (wide) 40.sp else 44.sp))
            Text(
                content.title,
                style = TextStyle(color = ink, fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                maxLines = 2,
            )
            if (wide && content.message.isNotBlank()) {
                Text(
                    content.message,
                    modifier = GlanceModifier.padding(top = 4.dp),
                    style = TextStyle(color = inkSoft, fontSize = 12.sp, textAlign = TextAlign.Center),
                    maxLines = 3,
                )
            }
        }
    }
}
