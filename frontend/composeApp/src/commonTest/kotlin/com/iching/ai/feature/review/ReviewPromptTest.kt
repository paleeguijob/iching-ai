package com.iching.ai.feature.review

import com.iching.ai.domain.model.AppSettings
import com.iching.ai.domain.model.withReadingCompleted
import com.iching.ai.domain.model.withReviewRequested
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReviewPromptTest {
    private val settings = AppSettings(completedReadings = 3, firstOpenEpochDay = 100)
    private val today = 102L

    @Test
    fun asksOnceThresholdsAreMet() = assertTrue(shouldRequestReview(settings, today, posterShownThisRun = false))

    @Test
    fun waitsForThreeReadings() =
        assertFalse(shouldRequestReview(settings.copy(completedReadings = 2), today, posterShownThisRun = false))

    @Test
    fun waitsTwoDaysAfterInstall() {
        assertFalse(shouldRequestReview(settings, 101, posterShownThisRun = false))
        assertFalse(shouldRequestReview(settings.copy(firstOpenEpochDay = null), today, posterShownThisRun = false))
    }

    @Test
    fun waitsThirtyDaysBetweenAsks() {
        assertFalse(shouldRequestReview(settings.copy(lastReviewRequestEpochDay = 80), 109, posterShownThisRun = false))
        assertTrue(shouldRequestReview(settings.copy(lastReviewRequestEpochDay = 72), today, posterShownThisRun = false))
    }

    @Test
    fun neverAsksAfterAPosterThisRun() = assertFalse(shouldRequestReview(settings, today, posterShownThisRun = true))

    @Test
    fun helpersCountReadingsAndRememberTheAsk() {
        assertEquals(1, AppSettings().withReadingCompleted().completedReadings)
        assertEquals(105L, AppSettings().withReviewRequested(105).lastReviewRequestEpochDay)
    }
}
