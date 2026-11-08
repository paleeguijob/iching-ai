package com.iching.ai.domain.safety

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CrisisCheckTest {
    @Test
    fun selfHarmQuestionsGetTheHotlineMessage() {
        assertEquals(SELF_HARM_MESSAGE, crisisMessage("ช่วงนี้ อยาก ตาย มากเลย"))
        assertEquals(SELF_HARM_MESSAGE, crisisMessage("I want to Kill  myself"))
    }

    @Test
    fun medicalEmergenciesGet1669() {
        assertEquals(MEDICAL_MESSAGE, crisisMessage("แน่นหน้าอกมาก หายใจไม่ออก"))
    }

    @Test
    fun ordinaryAndSensitiveQuestionsPass() {
        assertNull(crisisMessage("งานใหม่จะดีไหม"))
        assertNull(crisisMessage("ควรไปหาหมอเรื่องโรคกระเพาะไหม"))
        assertNull(crisisMessage(""))
    }
}
