package com.iching.ai.ui.theme

import androidx.compose.ui.graphics.Color
import com.iching.ai.domain.model.AppTheme

data class ZenPalette(
    val paperCream: Color,
    val paperMid: Color,
    val paperShade: Color,
    val paperDeep: Color,
    val inkBlack: Color,
    val inkSoft: Color,
    val inkFaint: Color,
    val bambooGreen: Color,
    val bambooDeep: Color,
    val cinnabarRed: Color,
    val cinnabarSoft: Color,
    val scroll: Color,
    val scrollEdge: Color,
    val scrollWood: Color,
    val scrollRollerLight: Color,
    val scrollRollerDark: Color,
    val secondaryContainer: Color,
    val surfaceLow: Color,
    val outline: Color,
    val outlineVariant: Color,
    val isDark: Boolean = false,
)

/** Palette from the spec: Zen minimalist ink on paper. Share cards always use this one. */
val InkPaperPalette = ZenPalette(
    paperCream = Color(0xFFFAF9F6),
    paperMid = Color(0xFFF7F4EC),
    paperShade = Color(0xFFF1EEE6),
    paperDeep = Color(0xFFE7E1D3),
    inkBlack = Color(0xFF2D3748),
    inkSoft = Color(0xFF5A6578),
    inkFaint = Color(0xFF9AA1AD),
    bambooGreen = Color(0xFF8F9779),
    bambooDeep = Color(0xFF6B7357),
    cinnabarRed = Color(0xFF9C4221),
    cinnabarSoft = Color(0xFFC9785A),
    scroll = Color(0xFFF6F0E1),
    scrollEdge = Color(0xFFF3EBD7),
    scrollWood = Color(0xFF5B4636),
    scrollRollerLight = Color(0xFF7A5E48),
    scrollRollerDark = Color(0xFF3E2E22),
    secondaryContainer = Color(0xFFE6E8DC),
    surfaceLow = Color(0xFFF6F4EE),
    outline = Color(0xFFCFC8B8),
    outlineVariant = Color(0xFFE3DDCF),
)

private val BambooPalette = ZenPalette(
    paperCream = Color(0xFFF6F9F1),
    paperMid = Color(0xFFF0F5E8),
    paperShade = Color(0xFFE9F0DF),
    paperDeep = Color(0xFFDCE6CE),
    inkBlack = Color(0xFF26352B),
    inkSoft = Color(0xFF4C5F50),
    inkFaint = Color(0xFF8C9B8E),
    bambooGreen = Color(0xFF7A9A6A),
    bambooDeep = Color(0xFF4F7048),
    cinnabarRed = Color(0xFF9C4221),
    cinnabarSoft = Color(0xFFC9785A),
    scroll = Color(0xFFF2F5E5),
    scrollEdge = Color(0xFFE9EED8),
    scrollWood = Color(0xFF4A5A3C),
    scrollRollerLight = Color(0xFF66785A),
    scrollRollerDark = Color(0xFF34402A),
    secondaryContainer = Color(0xFFDFE9D2),
    surfaceLow = Color(0xFFF1F5EA),
    outline = Color(0xFFC3CFB4),
    outlineVariant = Color(0xFFDBE3CE),
)

private val CinnabarDawnPalette = ZenPalette(
    paperCream = Color(0xFFFCF5EF),
    paperMid = Color(0xFFF9EEE5),
    paperShade = Color(0xFFF6E9DE),
    paperDeep = Color(0xFFECD8C8),
    inkBlack = Color(0xFF3A2A26),
    inkSoft = Color(0xFF6B524B),
    inkFaint = Color(0xFFA8958D),
    bambooGreen = Color(0xFFA08A5E),
    bambooDeep = Color(0xFF7A6A45),
    cinnabarRed = Color(0xFFA83A1F),
    cinnabarSoft = Color(0xFFD98A6C),
    scroll = Color(0xFFFAF0E0),
    scrollEdge = Color(0xFFF6E6D0),
    scrollWood = Color(0xFF6B3F2E),
    scrollRollerLight = Color(0xFF8A5844),
    scrollRollerDark = Color(0xFF4A2A1E),
    secondaryContainer = Color(0xFFF0E3CF),
    surfaceLow = Color(0xFFFAF0E8),
    outline = Color(0xFFD9C4B2),
    outlineVariant = Color(0xFFEBDCCD),
)

private val InkNightPalette = ZenPalette(
    paperCream = Color(0xFF171B22),
    paperMid = Color(0xFF1B2028),
    paperShade = Color(0xFF20252E),
    paperDeep = Color(0xFF2B313C),
    inkBlack = Color(0xFFE8E4DA),
    inkSoft = Color(0xFFB4B2AB),
    inkFaint = Color(0xFF7C8088),
    bambooGreen = Color(0xFF8E9A76),
    bambooDeep = Color(0xFFB6C29C),
    cinnabarRed = Color(0xFFE08A63),
    cinnabarSoft = Color(0xFFC9785A),
    scroll = Color(0xFF232832),
    scrollEdge = Color(0xFF1D222B),
    scrollWood = Color(0xFF14110D),
    scrollRollerLight = Color(0xFF3A3028),
    scrollRollerDark = Color(0xFF0E0C09),
    secondaryContainer = Color(0xFF2E3529),
    surfaceLow = Color(0xFF1B2028),
    outline = Color(0xFF454C58),
    outlineVariant = Color(0xFF323843),
    isDark = true,
)

fun paletteFor(theme: AppTheme): ZenPalette = when (theme) {
    AppTheme.INK_PAPER -> InkPaperPalette
    AppTheme.BAMBOO -> BambooPalette
    AppTheme.CINNABAR_DAWN -> CinnabarDawnPalette
    AppTheme.INK_NIGHT -> InkNightPalette
}
