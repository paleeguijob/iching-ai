package com.iching.ai.platform

import android.content.Context
import com.google.android.play.core.review.ReviewManagerFactory

/**
 * Google Play In-App Review. Play decides whether the sheet actually shows (it has its own quota) and never says
 * whether the seeker rated, so success only means the flow was handed to Play. Shows only for installs from Play.
 */
class AndroidReview(private val context: Context, private val activities: ActivityTracker) : NativeReview {
    private val manager by lazy { ReviewManagerFactory.create(context) }

    override fun requestReview(callback: ResultCallback) {
        manager.requestReviewFlow().addOnCompleteListener { request ->
            val activity = activities.current
            when {
                !request.isSuccessful -> callback.onResult(null, request.exception?.message ?: "review flow unavailable")
                activity == null -> callback.onResult(null, "no foreground activity")
                else -> manager.launchReviewFlow(activity, request.result).addOnCompleteListener { callback.onResult("ok", null) }
            }
        }
    }

    /** The Play app's own page first (straight to the review box), the web page where Play is missing. */
    override fun storeUrls(): List<String> = listOf(
        "market://details?id=${context.packageName}",
        "https://play.google.com/store/apps/details?id=${context.packageName}",
    )

    override fun platform() = "android"
}
