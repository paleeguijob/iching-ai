package com.iching.ai.ui.strings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HomePlaceholderTest {
    @Test
    fun englishPlaceholderFitsOneLineOnA360dpPhone() {
        assertTrue(EnHomeStrings.askPlaceholder.length <= 40, EnHomeStrings.askPlaceholder)
    }

    @Test
    fun thaiPlaceholderIsUnchanged() {
        assertEquals("พิมพ์คำถาม หรือเว้นว่างไว้อธิษฐานในใจ", ThHomeStrings.askPlaceholder)
    }
}
