package com.iching.ai.platform

import com.iching.ai.domain.model.AppLanguage

/**
 * The FCM topic of the daily hexagram push per language. Keep in step with DAILY_TOPICS in
 * backend/functions/src/core/daily.ts. The Thai topic is the one 1.x already subscribed to.
 */
fun dailyTopicFor(lang: AppLanguage): String = when (lang) {
    AppLanguage.TH -> "daily-hexagram-th"
    AppLanguage.EN -> "daily-hexagram-en"
}

/** The topic of the language the seeker is not reading, which they must leave. */
fun otherTopic(lang: AppLanguage): String =
    dailyTopicFor(if (lang == AppLanguage.EN) AppLanguage.TH else AppLanguage.EN)
