package com.iching.ai.platform

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Native SDKs (Firebase, RevenueCat, AdMob) are called from platform code — Kotlin in
 * androidApp, Swift in iosApp — and handed to shared code through these interfaces.
 * Callbacks use a plain interface (not a Kotlin lambda) so Swift can implement them cleanly.
 *
 * Android: androidApp/src/main/kotlin/com/iching/ai/platform/
 * iOS:     iosApp/iosApp/Bridges/
 */
interface ResultCallback {
    /** Exactly one of [value] / [error] is non-null. */
    fun onResult(value: String?, error: String?)
}

interface NativeAuth {
    fun currentUserId(): String?

    /** "anonymous", "apple.com" or "google.com". */
    fun currentProvider(): String?
    fun currentEmail(): String?
    fun signInAnonymously(callback: ResultCallback)

    /** value = Firebase ID token. */
    fun idToken(forceRefresh: Boolean, callback: ResultCallback)

    /** value = App Check token (Play Integrity / App Attest). */
    fun appCheckToken(callback: ResultCallback)

    /** Shows the provider sheet and links it to the current anonymous user; if that Apple/Google
     *  account already has a user here, signs into it instead. value = resulting uid. */
    fun linkApple(callback: ResultCallback)
    fun linkGoogle(callback: ResultCallback)
    fun supportsApple(): Boolean
    fun supportsGoogle(): Boolean
    fun signOut()

    /** Stable per-install id (Android: ANDROID_ID, iOS: identifierForVendor). */
    fun deviceId(): String
    fun appVersion(): String
}

interface NativePurchases {
    /** Identifies the RevenueCat customer with the Firebase uid. value = "ok". */
    fun logIn(uid: String, callback: ResultCallback)

    /**
     * value = JSON array of
     * {"packageId","productId","kind":"monthly|yearly|coins","title","price","period","trial"}
     */
    fun offerings(callback: ResultCallback)

    /** value = "purchased" or "cancelled". */
    fun purchase(packageId: String, callback: ResultCallback)

    /** value = "ok". */
    fun restore(callback: ResultCallback)
    fun manageSubscriptionsUrl(): String
    fun logOut()
}

interface NativeAds {
    /** Shows a rewarded ad. value = "rewarded" when the reward was earned, "dismissed" otherwise. */
    fun showRewarded(callback: ResultCallback)
    fun preload()
}

interface NativeAnalytics {
    fun logEvent(name: String, params: Map<String, String>)
    fun setUserId(uid: String?)
    fun recordError(message: String)
}

class NativeBridges(
    val auth: NativeAuth,
    val purchases: NativePurchases,
    val ads: NativeAds,
    val analytics: NativeAnalytics,
)

class NativeException(message: String) : Exception(message)

/** Bridges a callback-style native call into a suspend function. */
suspend fun awaitNative(block: (ResultCallback) -> Unit): String = suspendCancellableCoroutine { cont ->
    block(object : ResultCallback {
        override fun onResult(value: String?, error: String?) {
            if (!cont.isActive) return
            if (value != null) cont.resume(value) else cont.resumeWith(Result.failure(NativeException(error ?: "Unknown native error")))
        }
    })
}
