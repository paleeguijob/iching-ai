package com.iching.ai.feature.home

import com.iching.ai.ui.strings.TH_STRINGS
import com.iching.ai.feature.siamsi.DrawButton
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class SiamSiArtHintTest {
    @Test
    fun tappingTheCylinderStartsAFreeShake() {
        assertNull(siamsiArtHint(DrawButton.SHAKE, TH_STRINGS.home))
    }

    @Test
    fun tappingTheCylinderNeverOpensAnAd() {
        val hint = assertNotNull(siamsiArtHint(DrawButton.WATCH_AD, TH_STRINGS.home))
        assertEquals("กดปุ่มด้านล่างเพื่อดูโฆษณาแล้วเขย่า", hint)
    }

    @Test
    fun tappingTheCylinderNeverOpensThePaywall() {
        assertNotNull(siamsiArtHint(DrawButton.PAYWALL, TH_STRINGS.home))
    }

    @Test
    fun aDisabledCylinderNeedsNoHint() {
        assertNull(siamsiArtHint(DrawButton.DISABLED, TH_STRINGS.home))
    }
}
