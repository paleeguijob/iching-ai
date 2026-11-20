package com.iching.ai.platform

import com.iching.ai.domain.model.AppLanguage
import com.iching.ai.domain.model.AppSettings
import com.iching.ai.domain.model.languageFor
import kotlin.test.Test
import kotlin.test.assertEquals

class DailyTopicTest {
    @Test
    fun topicsMatchTheServer() {
        assertEquals("daily-hexagram-th", dailyTopicFor(AppLanguage.TH))
        assertEquals("daily-hexagram-en", dailyTopicFor(AppLanguage.EN))
    }

    @Test
    fun otherTopicIsTheOtherLanguage() {
        assertEquals("daily-hexagram-th", otherTopic(AppLanguage.EN))
        assertEquals("daily-hexagram-en", otherTopic(AppLanguage.TH))
    }

    @Test
    fun killSwitchSendsAnEnglishSeekerToTheThaiTopic() {
        val effective = AppSettings(language = AppLanguage.EN).languageFor(englishEnabled = false)
        assertEquals("daily-hexagram-th", dailyTopicFor(effective))
    }
}
