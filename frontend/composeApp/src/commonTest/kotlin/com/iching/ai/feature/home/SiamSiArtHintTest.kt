package com.iching.ai.feature.home

import com.iching.ai.feature.siamsi.DrawButton
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class SiamSiArtHintTest {
    @Test
    fun tappingTheCylinderStartsAFreeShake() {
        assertNull(siamsiArtHint(DrawButton.SHAKE))
    }

    @Test
    fun tappingTheCylinderNeverOpensAnAd() {
        val hint = assertNotNull(siamsiArtHint(DrawButton.WATCH_AD))
        assertEquals("กดปุ่มด้านล่างเพื่อดูโฆษณาแล้วเขย่า", hint)
    }

    @Test
    fun tappingTheCylinderNeverOpensThePaywall() {
        assertNotNull(siamsiArtHint(DrawButton.PAYWALL))
    }

    @Test
    fun aDisabledCylinderNeedsNoHint() {
        assertNull(siamsiArtHint(DrawButton.DISABLED))
    }
}
