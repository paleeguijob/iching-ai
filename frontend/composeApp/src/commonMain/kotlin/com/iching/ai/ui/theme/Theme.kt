package com.iching.ai.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.iching.ai.resources.Res
import com.iching.ai.resources.charm_400
import com.iching.ai.resources.charm_700
import com.iching.ai.resources.noto_serif_thai_400
import com.iching.ai.resources.noto_serif_thai_600
import com.iching.ai.resources.noto_serif_thai_700
import org.jetbrains.compose.resources.Font

/** Palette from the spec: Zen minimalist ink on paper. */
object ZenColors {
    val PaperCream = Color(0xFFFAF9F6)
    val PaperShade = Color(0xFFF1EEE6)
    val PaperDeep = Color(0xFFE7E1D3)
    val InkBlack = Color(0xFF2D3748)
    val InkSoft = Color(0xFF5A6578)
    val InkFaint = Color(0xFF9AA1AD)
    val BambooGreen = Color(0xFF8F9779)
    val BambooDeep = Color(0xFF6B7357)
    val CinnabarRed = Color(0xFF9C4221)
    val CinnabarSoft = Color(0xFFC9785A)
    val Scroll = Color(0xFFF6F0E1)
    val ScrollWood = Color(0xFF5B4636)
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
    val colors = lightColorScheme(
        primary = ZenColors.InkBlack,
        onPrimary = ZenColors.PaperCream,
        primaryContainer = ZenColors.PaperDeep,
        onPrimaryContainer = ZenColors.InkBlack,
        secondary = ZenColors.BambooGreen,
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFE6E8DC),
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
        surfaceContainerLow = Color(0xFFF6F4EE),
        surfaceContainerHigh = ZenColors.PaperDeep,
        outline = Color(0xFFCFC8B8),
        outlineVariant = Color(0xFFE3DDCF),
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
        content = content,
    )
}
