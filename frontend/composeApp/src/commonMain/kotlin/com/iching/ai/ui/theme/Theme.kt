package com.iching.ai.ui.theme

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.iching.ai.domain.model.AppTheme
import com.iching.ai.resources.Res
import com.iching.ai.resources.charm_400
import com.iching.ai.resources.charm_700
import com.iching.ai.resources.noto_serif_thai_400
import com.iching.ai.resources.noto_serif_thai_600
import com.iching.ai.resources.noto_serif_thai_700
import org.jetbrains.compose.resources.Font

/** The selected theme's colours. Backed by snapshot state, so screens recompose when the theme changes. */
object ZenColors {
    private var palette by mutableStateOf(InkPaperPalette)

    fun useTheme(theme: AppTheme) {
        palette = paletteFor(theme)
    }

    val current: ZenPalette get() = palette
    val PaperCream get() = palette.paperCream
    val PaperMid get() = palette.paperMid
    val PaperShade get() = palette.paperShade
    val PaperDeep get() = palette.paperDeep
    val InkBlack get() = palette.inkBlack
    val InkSoft get() = palette.inkSoft
    val InkFaint get() = palette.inkFaint
    val BambooGreen get() = palette.bambooGreen
    val BambooDeep get() = palette.bambooDeep
    val CinnabarRed get() = palette.cinnabarRed
    val CinnabarSoft get() = palette.cinnabarSoft
    val Scroll get() = palette.scroll
    val ScrollEdge get() = palette.scrollEdge
    val ScrollWood get() = palette.scrollWood
    val ScrollRollerLight get() = palette.scrollRollerLight
    val ScrollRollerDark get() = palette.scrollRollerDark
}

@Composable
fun bodyFontFamily() = FontFamily(
    Font(Res.font.noto_serif_thai_400, FontWeight.Normal),
    Font(Res.font.noto_serif_thai_600, FontWeight.SemiBold),
    Font(Res.font.noto_serif_thai_700, FontWeight.Bold),
)

@Composable
fun displayFontFamily() = FontFamily(
    Font(Res.font.charm_400, FontWeight.Normal),
    Font(Res.font.charm_700, FontWeight.Bold),
)

@Composable
private fun zenTypography(): Typography {
    val body = bodyFontFamily()
    val display = displayFontFamily()
    return Typography(
        displayLarge = TextStyle(fontFamily = display, fontWeight = FontWeight.Bold, fontSize = 46.sp, lineHeight = 64.sp),
        displayMedium = TextStyle(fontFamily = display, fontWeight = FontWeight.Bold, fontSize = 36.sp, lineHeight = 52.sp),
        displaySmall = TextStyle(fontFamily = display, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 44.sp),
        headlineLarge = TextStyle(fontFamily = display, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 44.sp),
        headlineMedium = TextStyle(fontFamily = display, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 38.sp),
        headlineSmall = TextStyle(fontFamily = body, fontWeight = FontWeight.SemiBold, fontSize = 21.sp, lineHeight = 32.sp),
        titleLarge = TextStyle(fontFamily = body, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 32.sp),
        titleMedium = TextStyle(fontFamily = body, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 28.sp),
        titleSmall = TextStyle(fontFamily = body, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 24.sp),
        bodyLarge = TextStyle(fontFamily = body, fontWeight = FontWeight.Normal, fontSize = 17.sp, lineHeight = 31.sp),
        bodyMedium = TextStyle(fontFamily = body, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 26.sp),
        bodySmall = TextStyle(fontFamily = body, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 21.sp),
        labelLarge = TextStyle(fontFamily = body, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 22.sp),
        labelMedium = TextStyle(fontFamily = body, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 19.sp),
        labelSmall = TextStyle(fontFamily = body, fontWeight = FontWeight.Normal, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp),
    )
}

@Composable
fun ZenTheme(content: @Composable () -> Unit) {
    val palette = ZenColors.current
    SystemBarsEffect(darkIcons = !palette.isDark)
    val colors = lightColorScheme(
        primary = ZenColors.InkBlack,
        onPrimary = ZenColors.PaperCream,
        primaryContainer = ZenColors.PaperDeep,
        onPrimaryContainer = ZenColors.InkBlack,
        secondary = ZenColors.BambooGreen,
        onSecondary = if (palette.isDark) palette.paperCream else Color.White,
        secondaryContainer = palette.secondaryContainer,
        onSecondaryContainer = ZenColors.BambooDeep,
        tertiary = ZenColors.CinnabarRed,
        onTertiary = ZenColors.PaperCream,
        background = ZenColors.PaperCream,
        onBackground = ZenColors.InkBlack,
        surface = ZenColors.PaperCream,
        onSurface = ZenColors.InkBlack,
        surfaceVariant = ZenColors.PaperShade,
        onSurfaceVariant = ZenColors.InkSoft,
        surfaceContainer = ZenColors.PaperShade,
        surfaceContainerLow = palette.surfaceLow,
        surfaceContainerHigh = ZenColors.PaperDeep,
        outline = palette.outline,
        outlineVariant = palette.outlineVariant,
        error = ZenColors.CinnabarRed,
    )
    MaterialTheme(
        colorScheme = colors,
        typography = zenTypography(),
        shapes = Shapes(
            small = RoundedCornerShape(8.dp),
            medium = RoundedCornerShape(14.dp),
            large = RoundedCornerShape(22.dp),
        ),
    ) {
        // The app has no Surface to set this: without it, uncoloured Text falls back to black on every theme.
        CompositionLocalProvider(LocalContentColor provides ZenColors.InkBlack, content = content)
    }
}
