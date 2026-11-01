package com.iching.ai.platform

import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.getOfferingsWith
import com.revenuecat.purchases.logInWith
import com.revenuecat.purchases.logOutWith
import com.revenuecat.purchases.purchaseWith
import com.revenuecat.purchases.restorePurchasesWith
import org.json.JSONArray
import org.json.JSONObject

/**
 * RevenueCat on Android. Products come from the *current* offering in the RevenueCat dashboard:
 * a Monthly package, an Annual package and a custom package for the coin pack
 * (product id containing "coins", e.g. iching_coins_10).
 */
class AndroidPurchases(
    private val activities: ActivityTracker,
    private val packageName: String,
    private val configured: Boolean,
) : NativePurchases {

    private fun notConfigured(callback: ResultCallback) = callback.onResult(null, "RevenueCat is not configured (REVENUECAT_ANDROID_KEY)")

    override fun logIn(uid: String, callback: ResultCallback) {
        if (!configured) return notConfigured(callback)
        Purchases.sharedInstance.logInWith(
            uid,
            onError = { callback.onResult(null, it.message) },
            onSuccess = { _, _ -> callback.onResult("ok", null) },
        )
    }

    override fun offerings(callback: ResultCallback) {
        if (!configured) return notConfigured(callback)
        Purchases.sharedInstance.getOfferingsWith(
            onError = { callback.onResult(null, it.message) },
            onSuccess = { offerings ->
                val out = JSONArray()
                offerings.current?.availablePackages.orEmpty().forEach { pkg ->
                    val product = pkg.product
                    val kind = when {
                        pkg.packageType == PackageType.MONTHLY -> "monthly"
                        pkg.packageType == PackageType.ANNUAL -> "yearly"
                        product.id.contains("coins") -> "coins"
                        else -> return@forEach
                    }
                    val trialDays = product.defaultOption?.freePhase?.billingPeriod?.let { it.value * daysPer(it.unit.name) }
                    out.put(
                        JSONObject()
                            .put("packageId", pkg.identifier)
                            .put("productId", product.id)
                            .put("kind", kind)
                            .put("title", product.title)
                            .put("price", product.price.formatted)
                            .put("period", if (kind == "monthly") "เดือน" else if (kind == "yearly") "ปี" else JSONObject.NULL)
                            .put("trial", trialDays?.let { "ทดลองฟรี $it วัน" } ?: JSONObject.NULL),
                    )
                }
                callback.onResult(out.toString(), null)
            },
        )
    }

    private fun daysPer(unit: String) = when (unit) {
        "WEEK" -> 7
        "MONTH" -> 30
        "YEAR" -> 365
        else -> 1
    }

    override fun purchase(packageId: String, callback: ResultCallback) {
        if (!configured) return notConfigured(callback)
        val activity = activities.current ?: return callback.onResult(null, "no activity")
        Purchases.sharedInstance.getOfferingsWith(
            onError = { callback.onResult(null, it.message) },
            onSuccess = { offerings ->
                val pkg = offerings.current?.availablePackages?.firstOrNull { it.identifier == packageId }
                    ?: return@getOfferingsWith callback.onResult(null, "package not found")
                Purchases.sharedInstance.purchaseWith(
                    PurchaseParams.Builder(activity, pkg).build(),
                    onError = { error, userCancelled ->
                        if (userCancelled) callback.onResult("cancelled", null) else callback.onResult(null, error.message)
                    },
                    onSuccess = { _, _ -> callback.onResult("purchased", null) },
                )
            },
        )
    }

    override fun restore(callback: ResultCallback) {
        if (!configured) return notConfigured(callback)
        Purchases.sharedInstance.restorePurchasesWith(
            onError = { callback.onResult(null, it.message) },
            onSuccess = { callback.onResult("ok", null) },
        )
    }

    override fun manageSubscriptionsUrl(): String =
        "https://play.google.com/store/account/subscriptions?package=$packageName"

    override fun logOut() {
        if (!configured) return
        Purchases.sharedInstance.logOutWith(onError = {}, onSuccess = {})
    }
}
