package com.iching.ai.session

import com.iching.ai.data.api.ApiSession
import com.iching.ai.domain.model.Account
import com.iching.ai.domain.model.AccountStatus
import com.iching.ai.domain.model.ProductKind
import com.iching.ai.domain.model.SignInProvider
import com.iching.ai.domain.model.StoreProduct
import com.iching.ai.domain.repository.ApiException
import com.iching.ai.domain.repository.FailureReason
import com.iching.ai.domain.repository.JournalRepository
import com.iching.ai.domain.repository.OracleApi
import com.iching.ai.domain.repository.SettingsRepository
import com.iching.ai.platform.NativeAuth
import com.iching.ai.platform.NativeBridges
import com.iching.ai.platform.NativeException
import com.iching.ai.platform.awaitNative
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/** Supplies Firebase credentials to the HTTP client, signing in anonymously on first use. */
class FirebaseApiSession(private val auth: NativeAuth) : ApiSession {
    private val signInLock = Mutex()

    suspend fun ensureSignedIn(): String = signInLock.withLock {
        auth.currentUserId() ?: try {
            awaitNative { auth.signInAnonymously(it) }
        } catch (e: NativeException) {
            throw ApiException(FailureReason.UNAUTHENTICATED, e.message ?: "Sign-in failed")
        }
    }

    override suspend fun idToken(forceRefresh: Boolean): String {
        ensureSignedIn()
        return try {
            awaitNative { auth.idToken(forceRefresh, it) }
        } catch (e: NativeException) {
            throw ApiException(FailureReason.UNAUTHENTICATED, e.message ?: "No ID token")
        }
    }

    // A missing App Check token is not fatal here: the server decides whether to enforce it.
    override suspend fun appCheckToken(): String? =
        runCatching { awaitNative { auth.appCheckToken(it) } }.getOrNull()

    override val deviceId: String get() = auth.deviceId()
    override val appVersion: String get() = auth.appVersion()
}

sealed interface PurchaseResult {
    data class Success(val status: AccountStatus) : PurchaseResult
    data object Cancelled : PurchaseResult
    data class Error(val message: String) : PurchaseResult
}

sealed interface AdResult {
    data class Granted(val status: AccountStatus) : AdResult
    data object NotCompleted : AdResult
    data object LimitReached : AdResult
    data class Error(val message: String) : AdResult
}

@Serializable
private data class ProductJson(
    val packageId: String,
    val productId: String,
    val kind: String,
    val title: String = "",
    val price: String,
    val period: String? = null,
    val trial: String? = null,
)

/**
 * App-wide account state: who is signed in, what they may do today, and the actions that change
 * it (ads, purchases, linking Apple/Google, deleting the account). Screens observe [status].
 */
class AppSession(
    private val bridges: NativeBridges,
    private val apiSession: FirebaseApiSession,
    private val api: OracleApi,
    private val journal: JournalRepository,
    private val settings: SettingsRepository,
    private val scope: CoroutineScope,
) {
    private val _status = MutableStateFlow(AccountStatus.UNKNOWN_FREE)
    val status: StateFlow<AccountStatus> = _status.asStateFlow()

    private val _account = MutableStateFlow<Account?>(null)
    val account: StateFlow<Account?> = _account.asStateFlow()

    /** True once the server answered at least once this session. */
    private val _online = MutableStateFlow(false)
    val online: StateFlow<Boolean> = _online.asStateFlow()

    private val json = Json { ignoreUnknownKeys = true }
    private var started = false

    val supportsApple: Boolean get() = bridges.auth.supportsApple()
    val supportsGoogle: Boolean get() = bridges.auth.supportsGoogle()
    val manageSubscriptionsUrl: String get() = bridges.purchases.manageSubscriptionsUrl()

    fun start() {
        if (started) return
        started = true
        scope.launch {
            try {
                val uid = apiSession.ensureSignedIn()
                onSignedIn(uid)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                bridges.analytics.recordError("session_start: ${e.message}")
            }
            bridges.ads.preload()
        }
    }

    private suspend fun onSignedIn(uid: String) {
        _account.value = currentAccount()
        bridges.analytics.setUserId(uid)
        runCatching { awaitNative { bridges.purchases.logIn(uid, it) } }
        refreshStatus()
        if (_status.value.premium) syncJournal()
    }

    private fun currentAccount(): Account? {
        val uid = bridges.auth.currentUserId() ?: return null
        val provider = when (bridges.auth.currentProvider()) {
            "apple.com" -> SignInProvider.APPLE
            "google.com" -> SignInProvider.GOOGLE
            else -> SignInProvider.ANONYMOUS
        }
        return Account(uid, provider, bridges.auth.currentEmail())
    }

    fun updateStatus(status: AccountStatus?) {
        if (status != null) {
            _status.value = status
            _online.value = true
        }
    }

    suspend fun refreshStatus(): AccountStatus? = try {
        api.status().also(::updateStatus)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }

    /** Premium: copies server readings that are missing on this device (e.g. after reinstalling). */
    suspend fun syncJournal(): Int = try {
        journal.mergeFromServer(api.readings())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        0
    }

    suspend fun watchAdForReading(): AdResult {
        if (_status.value.adsWatchableLeft <= 0) return AdResult.LimitReached
        return try {
            val outcome = awaitNative { bridges.ads.showRewarded(it) }
            if (outcome != "rewarded") return AdResult.NotCompleted
            val (granted, status) = api.claimAdReward()
            updateStatus(status)
            log("ad_rewarded", mapOf("granted" to granted.toString()))
            if (granted) AdResult.Granted(status) else AdResult.LimitReached
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AdResult.Error(e.message ?: "")
        } finally {
            bridges.ads.preload()
        }
    }

    suspend fun products(): List<StoreProduct> {
        val raw = awaitNative { bridges.purchases.offerings(it) }
        return json.decodeFromString(ListSerializer(ProductJson.serializer()), raw).mapNotNull { p ->
            val kind = when (p.kind) {
                "monthly" -> ProductKind.MONTHLY
                "yearly" -> ProductKind.YEARLY
                "coins" -> ProductKind.COINS
                else -> return@mapNotNull null
            }
            StoreProduct(p.packageId, p.productId, kind, p.title, p.price, p.period, p.trial)
        }.sortedBy { it.kind.ordinal }
    }

    suspend fun purchase(product: StoreProduct): PurchaseResult = try {
        apiSession.ensureSignedIn()
        when (awaitNative { bridges.purchases.purchase(product.packageId, it) }) {
            "purchased" -> {
                // The webhook updates the server too; syncing now makes the change instant.
                val status = api.syncPurchases()
                updateStatus(status)
                log("purchase_completed", mapOf("product" to product.productId))
                if (status.premium) syncJournal()
                PurchaseResult.Success(status)
            }
            else -> PurchaseResult.Cancelled
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        PurchaseResult.Error(e.message ?: "")
    }

    suspend fun restorePurchases(): PurchaseResult = try {
        awaitNative { bridges.purchases.restore(it) }
        val status = api.syncPurchases()
        updateStatus(status)
        if (status.premium) syncJournal()
        PurchaseResult.Success(status)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        PurchaseResult.Error(e.message ?: "")
    }

    /** Links Apple/Google so purchases and the journal follow the seeker to a new device. */
    suspend fun link(provider: SignInProvider): Result<Account> = try {
        val uid = when (provider) {
            SignInProvider.APPLE -> awaitNative { bridges.auth.linkApple(it) }
            SignInProvider.GOOGLE -> awaitNative { bridges.auth.linkGoogle(it) }
            SignInProvider.ANONYMOUS -> error("Nothing to link")
        }
        onSignedIn(uid)
        log("account_linked", mapOf("provider" to provider.name.lowercase()))
        Result.success(currentAccount()!!)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    /** Deletes server data and the auth user, wipes this device, then starts a fresh anonymous session. */
    suspend fun deleteAccount(): Result<Unit> = try {
        api.deleteAccount()
        bridges.purchases.logOut()
        bridges.auth.signOut()
        journal.clear()
        settings.clear()
        _account.value = null
        _status.value = AccountStatus.UNKNOWN_FREE
        val uid = apiSession.ensureSignedIn()
        onSignedIn(uid)
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    fun log(event: String, params: Map<String, String> = emptyMap()) = bridges.analytics.logEvent(event, params)
}
