package com.iching.ai.domain.safety

import com.iching.ai.domain.model.AppLanguage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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

    @Test
    fun englishPhrasesAreCrisisInEitherLanguage() {
        assertEquals(SELF_HARM_MESSAGE_EN, crisisMessage("I want to die", AppLanguage.EN))
        assertEquals(SELF_HARM_MESSAGE, crisisMessage("I want to die"))
        assertEquals(SELF_HARM_MESSAGE_EN, crisisMessage("I'm thinking of killmyself", AppLanguage.EN))
        assertEquals(MEDICAL_MESSAGE_EN, crisisMessage("he is unconscious", AppLanguage.EN))
        assertEquals(MEDICAL_MESSAGE, crisisMessage("He is unconscious!"))
        assertEquals(MEDICAL_MESSAGE_EN, crisisMessage("I can't breathe", AppLanguage.EN))
    }

    @Test
    fun englishWordsInsideOtherWordsOrAcrossWordsAreNotCrisis() {
        assertNull(crisisMessage("Should I spend my life with him?", AppLanguage.EN))
        assertNull(crisisMessage("Should I spend it all on the trip?", AppLanguage.EN))
        assertNull(crisisMessage("Will my stroke of luck last?".replace("stroke", "strokes of"), AppLanguage.EN))
    }

    @Test
    fun idiomsWithStrokeHeartAttackAndUnconsciousAreNotCrisis() {
        for (q in listOf(
            "Will my stroke of luck last?", "A stroke of good fortune is coming, right?",
            "My heart attack of love scares me", "Why do my unconscious fears keep returning?",
        )) assertNull(crisisMessage(q, AppLanguage.EN), q)
    }

    @Test
    fun realEnglishEmergenciesAreStillCrisis() {
        for (q in listOf(
            "he had a stroke", "She is unconscious", "my dad is having a heart attack", "he was found unconscious",
            "hes unconscious", "she passed out and wont wake",
            "I found my mom unconscious", "found him unconscious on the floor", "my mother unconscious, what do I do",
            "Help! Unconscious child", "I think he's having a stroke, of course I'm scared", "he had a stroke of some kind",
            "my dad had a heart attack of some sort", "he passed out and won't wake up",
        )) assertEquals(MEDICAL_MESSAGE_EN, crisisMessage(q, AppLanguage.EN), q)
    }

    @Test
    fun englishRepliesNeverOfferThaiNumbers() {
        for (m in listOf(SELF_HARM_MESSAGE_EN, MEDICAL_MESSAGE_EN)) {
            assertFalse("1323" in m || "1669" in m)
            assertFalse(Regex("[\u0E00-\u0E7F]").containsMatchIn(m))
        }
    }
}
