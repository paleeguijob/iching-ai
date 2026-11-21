package com.iching.ai.platform

import kotlin.test.Test
import kotlin.test.assertEquals

class SpeechVoiceTagTest {
    @Test
    fun thaiTextGetsTheThaiVoiceWhateverTheUiLanguage() {
        assertEquals("th-TH", speechVoiceTag("ผู้แสวงหาเอ๋ย"))
        assertEquals("th-TH", speechVoiceTag("Qian 乾 — ความเข้มแข็ง"))
    }

    @Test
    fun otherTextGetsTheEnglishVoice() {
        assertEquals("en-US", speechVoiceTag("Seeker, the river finds its way."))
        assertEquals("en-US", speechVoiceTag(""))
    }
}
