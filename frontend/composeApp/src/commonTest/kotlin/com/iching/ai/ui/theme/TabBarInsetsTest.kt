package com.iching.ai.ui.theme

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

class TabBarInsetsTest {

    @Test
    fun ctaSitsAGapAboveTheMeasuredTabBar() =
        // A 67dp bar (larger than the old 60dp guess) must still leave the full 12dp gap.
        assertEquals(79.dp, ctaBottomPadding(imeBottom = 0.dp, tabBarHeight = 67.dp))

    @Test
    fun ctaRidesTheKeyboardWhenItIsTallerThanTheBar() =
        assertEquals(312.dp, ctaBottomPadding(imeBottom = 300.dp, tabBarHeight = 67.dp))

    @Test
    fun ctaNeverDipsBehindTheBarWhileTheKeyboardIsStillShort() =
        assertEquals(79.dp, ctaBottomPadding(imeBottom = 30.dp, tabBarHeight = 67.dp))

    @Test
    fun tabListsEndAGapAboveTheMeasuredBar() =
        assertEquals(92.dp, tabContentBottomPadding(tabBarHeight = 72.dp))
}
