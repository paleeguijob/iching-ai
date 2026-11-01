package com.iching.ai

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.iching.ai.di.initKoin
import com.iching.ai.platform.ActivityTracker
import com.iching.ai.platform.AndroidAds
import com.iching.ai.platform.AndroidAnalytics
import com.iching.ai.platform.AndroidAuth
import com.iching.ai.platform.AndroidPurchases
import com.iching.ai.platform.NativeBridges
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import org.koin.android.ext.koin.androidContext

class IChingApplication : Application() {

    val activities = ActivityTracker()
    lateinit var ads: AndroidAds
        private set

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(activities)

        FirebaseApp.initializeApp(this)
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
            if (BuildConfig.DEBUG) debugAppCheckFactory() else PlayIntegrityAppCheckProviderFactory.getInstance()
        )

        val revenueCatKey = BuildConfig.REVENUECAT_ANDROID_KEY
        if (revenueCatKey.isNotBlank()) {
            Purchases.configure(PurchasesConfiguration.Builder(this, revenueCatKey).build())
        }

        ads = AndroidAds(this, activities, BuildConfig.ADMOB_REWARDED_ANDROID)
        val bridges = NativeBridges(
            auth = AndroidAuth(this, activities, BuildConfig.VERSION_NAME),
            purchases = AndroidPurchases(activities, packageName, configured = revenueCatKey.isNotBlank()),
            ads = ads,
            analytics = AndroidAnalytics(this),
        )
        initKoin(bridges) {
            androidContext(this@IChingApplication)
        }
    }

    /**
     * Debug builds use App Check's debug provider: the debug token is printed to Logcat on first
     * run — register it in Firebase console → App Check → Apps → Manage debug tokens.
     * Loaded by reflection because the debug artifact is a debugImplementation dependency.
     */
    private fun debugAppCheckFactory(): com.google.firebase.appcheck.AppCheckProviderFactory =
        Class.forName("com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory")
            .getMethod("getInstance")
            .invoke(null) as com.google.firebase.appcheck.AppCheckProviderFactory
}
