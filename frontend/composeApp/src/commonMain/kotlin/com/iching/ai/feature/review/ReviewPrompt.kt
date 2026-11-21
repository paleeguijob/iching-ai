package com.iching.ai.feature.review

import com.iching.ai.domain.model.AppSettings

const val REVIEW_MIN_READINGS = 3
const val REVIEW_MIN_DAYS_INSTALLED = 2L
const val REVIEW_COOLDOWN_DAYS = 30L

/**
 * Whether to ask the store's own rating sheet now. Only seekers who have come back for a few readings are asked,
 * never on the first days, at most once per cooldown, and never in a run that already showed a poster popup.
 * Nothing asks the seeker's opinion first: store policy forbids sending only happy seekers to rate.
 */
fun shouldRequestReview(settings: AppSettings, today: Long, posterShownThisRun: Boolean): Boolean {
    if (posterShownThisRun || settings.completedReadings < REVIEW_MIN_READINGS) return false
    val firstOpen = settings.firstOpenEpochDay ?: return false
    if (today - firstOpen < REVIEW_MIN_DAYS_INSTALLED) return false
    val last = settings.lastReviewRequestEpochDay ?: return true
    return today - last >= REVIEW_COOLDOWN_DAYS
}
