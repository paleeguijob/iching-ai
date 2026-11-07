package com.iching.ai.domain.model

enum class SignInProvider { ANONYMOUS, APPLE, GOOGLE }

data class Account(
    val uid: String,
    val provider: SignInProvider,
    val email: String? = null,
)

/** A seasonal campaign as the server reports it. */
data class CampaignStatus(val id: String, val year: Int, val active: Boolean, val endsAtMillis: Long)

/** The campaign id analytics events carry: the server's, or the year's own when the server reports none. */
fun campaignIdFor(campaign: CampaignStatus?, year: Int): String = campaign?.id ?: "cny-$year"

/** The hexagram cast once per account for the year; [lines] hold values 6-9, [coins] the three coin values of each toss. */
data class YearHexagram(val year: Int, val primaryId: Int, val lines: List<Int>, val coins: List<List<Int>>)

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
    /** The Chinese New Year campaign; null when the server runs none. */
    val campaign: CampaignStatus? = null,
) {
    /** Readings that reset tomorrow; [coins] are kept apart because they never expire. */
    val readingsLeftToday: Int get() = (readingsLeft - coins).coerceAtLeast(0)

    companion object {
        /** Assumed before the first server answer arrives (or while offline). */
        val UNKNOWN_FREE = AccountStatus(
            premium = false, premiumUntilMillis = null, coins = 0, includedLeft = 0, adCreditsLeft = 0,
            adsWatchableLeft = 2, readingsLeft = 0, followUpsPerReading = 0, journalDays = 7, aiEnabled = true,
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
