package com.iching.ai.domain.model

enum class SignInProvider { ANONYMOUS, APPLE, GOOGLE }

data class Account(
    val uid: String,
    val provider: SignInProvider,
    val email: String? = null,
)

/** Quota and entitlements as reported by the server (GET /status). */
data class AccountStatus(
    val premium: Boolean,
    val premiumUntilMillis: Long?,
    val coins: Int,
    /** Readings left today without an ad or a coin. */
    val includedLeft: Int,
    val adCreditsLeft: Int,
    val adsWatchableLeft: Int,
    val readingsLeft: Int,
    val followUpsPerReading: Int,
    /** Days of journal history visible; null = unlimited (Premium). */
    val journalDays: Int?,
    val aiEnabled: Boolean,
) {
    companion object {
        /** Assumed before the first server answer arrives (or while offline). */
        val UNKNOWN_FREE = AccountStatus(
            premium = false, premiumUntilMillis = null, coins = 0, includedLeft = 1, adCreditsLeft = 0,
            adsWatchableLeft = 2, readingsLeft = 1, followUpsPerReading = 0, journalDays = 7, aiEnabled = true,
        )
    }
}

enum class ProductKind { MONTHLY, YEARLY, COINS }

/** A purchasable package as the store reports it (localized price and period). */
data class StoreProduct(
    val packageId: String,
    val productId: String,
    val kind: ProductKind,
    val title: String,
    val price: String,
    /** e.g. "เดือน" / "ปี"; null for coins. */
    val periodTh: String?,
    /** Free-trial description, e.g. "ทดลองฟรี 7 วัน"; null when none. */
    val trialTh: String?,
)
