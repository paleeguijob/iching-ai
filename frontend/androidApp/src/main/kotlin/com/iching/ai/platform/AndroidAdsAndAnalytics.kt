package com.iching.ai.platform

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean

/** Remembers the foreground activity, which ads, purchases and Google sign-in need. */
class ActivityTracker : Application.ActivityLifecycleCallbacks {
    private var ref: WeakReference<Activity>? = null
    val current: Activity? get() = ref?.get()

    override fun onActivityResumed(activity: Activity) {
        ref = WeakReference(activity)
    }
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        ref = WeakReference(activity)
    }
    override fun onActivityStarted(activity: Activity) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) {
        if (ref?.get() === activity) ref = null
    }
}

/**
 * AdMob rewarded ads. Ads are initialised only after the UMP consent flow says ads may be
 * requested (required by Google for users in the EEA/UK; a no-op elsewhere).
 */
class AndroidAds(
    private val context: Context,
    private val activities: ActivityTracker,
    private val adUnitId: String,
) : NativeAds {
    private var ad: RewardedAd? = null
    private val loading = AtomicBoolean(false)
    private val initialised = AtomicBoolean(false)

    /** Call from the activity once it is visible. */
    fun gatherConsent(activity: Activity) {
        val info = UserMessagingPlatform.getConsentInformation(activity)
        info.requestConsentInfoUpdate(
            activity,
            ConsentRequestParameters.Builder().build(),
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
                    if (info.canRequestAds()) initialise()
                }
            },
            { if (info.canRequestAds()) initialise() },
        )
        if (info.canRequestAds()) initialise()
    }

    private fun initialise() {
        if (initialised.getAndSet(true)) return
        MobileAds.initialize(context) { preload() }
    }

    override fun preload() {
        if (!initialised.get() || ad != null || !loading.compareAndSet(false, true)) return
        RewardedAd.load(context, adUnitId, AdRequest.Builder().build(), object : RewardedAdLoadCallback() {
            override fun onAdLoaded(loaded: RewardedAd) {
                ad = loaded
                loading.set(false)
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                loading.set(false)
            }
        })
    }

    override fun showRewarded(callback: ResultCallback) {
        val activity = activities.current ?: return callback.onResult(null, "no activity")
        val current = ad ?: run {
            preload()
            return callback.onResult(null, "no ad loaded")
        }
        ad = null
        var earned = false
        current.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                callback.onResult(if (earned) "rewarded" else "dismissed", null)
                preload()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                callback.onResult(null, error.message)
                preload()
            }
        }
        current.show(activity) { earned = true }
    }
}

class AndroidAnalytics(context: Context) : NativeAnalytics {
    private val analytics = FirebaseAnalytics.getInstance(context)

    override fun logEvent(name: String, params: Map<String, String>) {
        analytics.logEvent(name, Bundle().apply { params.forEach { (k, v) -> putString(k, v.take(100)) } })
    }

    override fun setUserId(uid: String?) {
        analytics.setUserId(uid)
        FirebaseCrashlytics.getInstance().setUserId(uid ?: "")
    }

    override fun recordError(message: String) {
        FirebaseCrashlytics.getInstance().recordException(RuntimeException(message))
    }
}
