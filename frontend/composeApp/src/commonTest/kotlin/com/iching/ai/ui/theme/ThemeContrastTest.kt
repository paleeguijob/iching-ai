package com.iching.ai.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.iching.ai.domain.model.AppTheme
import kotlin.math.max
import kotlin.math.min
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ThemeContrastTest {

    private fun contrast(a: Color, b: Color): Float {
        val la = a.luminance()
        val lb = b.luminance()
        return (max(la, lb) + 0.05f) / (min(la, lb) + 0.05f)
    }

    @Test
    fun mainTextIsReadableOnEveryTheme() = AppTheme.entries.forEach { theme ->
        val p = paletteFor(theme)
        listOf(
            "ink/paper" to (p.inkBlack to p.paperCream),
            "soft/paper" to (p.inkSoft to p.paperCream),
            "button" to (p.paperCream to p.inkBlack),
            "ink/scroll" to (p.inkBlack to p.scroll),
            "ink/scrollEdge" to (p.inkBlack to p.scrollEdge),
            "ink/shade" to (p.inkBlack to p.paperShade),
            "cinnabar/paper" to (p.cinnabarRed to p.paperCream),
        ).forEach { (name, pair) ->
            val ratio = contrast(pair.first, pair.second)
            assertTrue(ratio >= 4.5f, "$theme $name is only $ratio:1")
        }
    }

    @Test
    fun defaultThemeKeepsCurrentColors() {
        assertEquals(Color(0xFFFAF9F6), InkPaperPalette.paperCream)
        assertEquals(Color(0xFF2D3748), InkPaperPalette.inkBlack)
        assertEquals(Color(0xFF9C4221), InkPaperPalette.cinnabarRed)
        assertEquals(InkPaperPalette, paletteFor(AppTheme.INK_PAPER))
    }

    @Test
    fun onlyInkNightIsDark() {
        assertEquals(listOf(AppTheme.INK_NIGHT), AppTheme.entries.filter { paletteFor(it).isDark })
        assertFalse(InkPaperPalette.isDark)
    }

    @Test
    fun zenColorsFollowsTheSelectedTheme() {
        ZenColors.useTheme(AppTheme.INK_NIGHT)
        assertEquals(paletteFor(AppTheme.INK_NIGHT).paperCream, ZenColors.PaperCream)
        ZenColors.useTheme(AppTheme.INK_PAPER)
        assertEquals(InkPaperPalette.paperCream, ZenColors.PaperCream)
    }
}
