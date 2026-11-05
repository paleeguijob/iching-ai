# Pre-cast Offer Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** When a free seeker taps "เริ่มเสี่ยงทาย" with no reading left today, offer the rewarded ad (or Premium / coins when no ad is left) in a bottom sheet before the coins are tossed.

**Architecture:** A pure rule `preCastOffer(status, online)` decides between casting straight away and opening a sheet. `HomeViewModel` owns the sheet state and the actions (ad, coins, scripture-only, Premium, dismiss) through `AppSession`, and emits one-shot cast requests that `HomeScreen` turns into navigation. The server and the divination screen do not change, apart from a `placement` analytics parameter.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform 1.12 (Material3 `ModalBottomSheet`), Koin, kotlinx.coroutines test.

**Spec:** [docs/superpowers/specs/2026-11-05-precast-offer-design.md](../specs/2026-11-05-precast-offer-design.md)

## Global Constraints

- No server change. Ad credits come from `POST /ad-reward` via `AppSession.watchAdForReading()`, and the server charges them at `POST /divine`.
- No change to `DivinationViewModel` behaviour or `ReadingNoticeCard`. The after-cast card stays as the fallback. The only change there is the `placement` parameter (Task 4).
- Status refresh before deciding: at most **1,500 ms**, then fall back to `session.status.value`.
- Thai numerals in the copy (`toThaiNumerals()`), as on the home screen.
- Coin price and trial text always come from the store (`session.products()`). Hide them while loading or on failure, but keep the buttons tappable.
- All user-visible copy is verbatim from spec sections 3–5. The spec is the source of truth for every Thai string.
- Run tests with `cd frontend && ./gradlew :composeApp:testAndroidHostTest`. iOS must still compile: `./gradlew :composeApp:compileKotlinIosSimulatorArm64`.

## Review Focus

1. **Double tap on "เริ่มเสี่ยงทาย" while status is refreshing.** Expect one sheet or one cast, never two navigations. Covered by `startIsIgnoredWhileDeciding` (Task 3).
2. **Sheet dismissed while a coin purchase is still in the store dialog.** On success the seeker must not be thrown into casting, because they backed out. Covered by `dismissedSheetDoesNotCastAfterPurchase` (Task 3).
3. **Status refresh hangs (bad network).** Decide with the old status after 1,500 ms. Covered by `slowRefreshFallsBackToKnownStatus` (Task 3).
4. **The question typed before tapping is kept.** The cast request carries the question and category captured at `onStart`, not later edits. Covered by `adGrantedCastsWithTheOriginalQuestion` (Task 3).
5. **Paywall purchase while the sheet is open.** The sheet turns into READY and does not navigate by itself. Covered by `sheetTurnsReadyWhenReadingsAppear` (Task 3).

---

### Task 1: Pre-cast rule

**Files:**
- Create: `frontend/composeApp/src/commonMain/kotlin/com/iching/ai/feature/home/PreCastGate.kt`
- Test: `frontend/composeApp/src/commonTest/kotlin/com/iching/ai/feature/home/PreCastGateTest.kt`

**Interfaces:**
- Produces: `enum class PreCastOffer { NONE, AD, UPSELL }` and `fun preCastOffer(status: AccountStatus, online: Boolean): PreCastOffer`

- [ ] **Step 1: Write the failing tests.** Base the free status on `AccountStatus.UNKNOWN_FREE.copy(includedLeft = 0, readingsLeft = 0, adsWatchableLeft = 2)`, and add one test per row of spec section 2, in order:
  - `offlineCastsStraightAway`: online=false with the free-out status → `NONE`
  - `aiOffCastsStraightAway`: `aiEnabled = false` → `NONE`
  - `premiumOutOfReadingsCastsStraightAway`: `premium = true, readingsLeft = 0, adsWatchableLeft = 0` → `NONE`
  - `readingsLeftCastsStraightAway`: `readingsLeft = 1` (an unused ad credit) → `NONE`
  - `coinsCastStraightAway`: `coins = 3, readingsLeft = 3, adsWatchableLeft = 0` → `NONE`
  - `freeOutWithAdsOffersTheAd`: the base status → `AD`
  - `freeWithNothingLeftOffersUpsell`: `adsWatchableLeft = 0` → `UPSELL`
- [ ] **Step 2: Run them and confirm they fail.** Command: `./gradlew :composeApp:testAndroidHostTest --tests '*PreCastGateTest*'`. Expected: compilation fails because `preCastOffer` is undefined.
- [ ] **Step 3: Implement `preCastOffer`.** Use a `when`, checking in the spec's row order.
- [ ] **Step 4: Run the tests and confirm they pass.** Same command. Expected: 7 passed.
- [ ] **Step 5: Commit.** Message: `Add the rule for offering the ad or a pack before casting`

### Task 2: Home quota bar copy

**Files:**
- Modify: `frontend/composeApp/src/commonMain/kotlin/com/iching/ai/feature/home/HomeScreen.kt` (`guidanceQuota`, the `adsWatchableLeft > 0` branch)
- Test: `frontend/composeApp/src/commonTest/kotlin/com/iching/ai/feature/home/GuidanceQuotaTest.kt` (`freeAtZeroShowsTheAdsLeftInsteadOfZero`)

- [ ] **Step 1: Change the test's expected note** to `"กดเริ่มเสี่ยงทาย แล้วชมโฆษณาสั้นก่อนโยนเหรียญ"`.
- [ ] **Step 2: Run it and confirm it fails.** Command: `./gradlew :composeApp:testAndroidHostTest --tests '*GuidanceQuotaTest*'`. Expected: 1 failure on the note.
- [ ] **Step 3: Replace the note string** in `guidanceQuota` with the same text.
- [ ] **Step 4: Run it and confirm it passes.** Same command, all pass.
- [ ] **Step 5: Commit.** Message: `Say on home that the ad comes before the toss`

### Task 3: HomeViewModel pre-cast flow

**Files:**
- Modify: `frontend/composeApp/src/commonMain/kotlin/com/iching/ai/feature/home/HomeViewModel.kt`
- Modify: `frontend/composeApp/src/commonTest/kotlin/com/iching/ai/session/AppSessionTest.kt`, the shared fakes (see Step 1)
- Test: `frontend/composeApp/src/commonTest/kotlin/com/iching/ai/feature/home/HomeViewModelPreCastTest.kt`

**Interfaces:**
- Consumes: `preCastOffer`, `PreCastOffer` (Task 1). From `AppSession`: `refreshStatus(): AccountStatus?`, `status: StateFlow<AccountStatus>`, `online: StateFlow<Boolean>`, `watchAdForReading(): AdResult`, `products(): List<StoreProduct>`, `purchase(StoreProduct): PurchaseResult`, `log(String, Map<String,String>)`. Messages `PENDING_MESSAGE`, `AWAITING_PAYMENT_MESSAGE` (internal, `feature/paywall`).
- Produces (all in `HomeViewModel.kt`):
  ```kotlin
  data class CastRequest(val question: String, val category: QuestionCategory?)
  enum class PreCastSheetKind { AD, UPSELL, READY }
  data class PreCastSheetUi(
      val kind: PreCastSheetKind,
      val question: String,
      val adsLeft: Int = 0,
      val coins: StoreProduct? = null,
      val premiumTrial: String? = null,
      val busy: Boolean = false,
      val message: String? = null,
  )
  // HomeViewModel
  val sheet: StateFlow<PreCastSheetUi?>
  val castRequests: Flow<CastRequest>   // Channel(BUFFERED).receiveAsFlow()
  val deciding: StateFlow<Boolean>      // true while refreshing status after a tap
  fun onStart(question: String, category: QuestionCategory?)
  fun onWatchAd()
  fun onBuyCoins()
  fun onScriptureOnly()
  fun onPremium()          // logs only; the screen opens the paywall
  fun onSheetDismissed()
  fun onCastFromReady()
  ```
  `HomeViewModel` keeps its constructor (`getDaily, observeSettings, session`), so Koin's `viewModelOf(::HomeViewModel)` is unchanged.

- [ ] **Step 1: Make the shared fakes configurable.** These stay `internal` in `AppSessionTest.kt` and keep their current defaults, so existing tests are unaffected.
  - `FakeApi`: add `var adGrant: Pair<Boolean, AccountStatus> = false to AccountStatus.UNKNOWN_FREE` and return it from `claimAdReward()`. Also add `var statusDelayMillis = 0L`, which `status()` awaits with `delay` before answering.
  - `FakePurchases`: add `var offeringsJson = "[]"` and return it from `offerings`.
  - New `internal class FakeAds(var answer: String = "rewarded") : NativeAds`.
- [ ] **Step 2: Write the failing tests** in `HomeViewModelPreCastTest`. Follow `DivinationViewModelTest`'s setup: `StandardTestDispatcher`, `Dispatchers.setMain`, an `AppSession` over the fakes, and `session.start()` then `runCurrent()` so `online` becomes true. Collect `castRequests` into a list in `backgroundScope`. "Status" below means what the server reports: set `api.statusNow` before `session.start()`. Since `onStart` refreshes status, `statusNow` is also what the rule sees. The tests:
  - `readingLeftCastsWithoutSheet`: status `readingsLeft = 1`. `onStart("ถาม", null)` → `castRequests == [CastRequest("ถาม", null)]`, `sheet.value == null`
  - `freeOutOpensAdSheet`: `readingsLeft = 0, adsWatchableLeft = 2`. Expect `sheet.value?.kind == AD`, `adsLeft == 2`, no cast request
  - `adGrantedCastsWithTheOriginalQuestion`: AD sheet, `adGrant = true to status(readingsLeft = 1)`, `onWatchAd()` → sheet null, `castRequests == [CastRequest("ถาม", QuestionCategory.WORK)]`
  - `adNotFinishedStaysWithMessage`: `FakeAds("dismissed")` → sheet still AD, `message == "ต้องชมโฆษณาจนจบจึงจะได้รับสิทธิ์"`, no cast
  - `adLimitTurnsSheetIntoUpsell`: `adGrant = false to status(adsWatchableLeft = 0)` → kind UPSELL, `message == "วันนี้ใช้สิทธิ์ชมโฆษณาครบแล้ว"`
  - `nothingLeftOpensUpsellWithStorePrice`: `adsWatchableLeft = 0`, offerings JSON with a `coins` package priced `"฿29"` and a `yearly` one with trial `"ทดลองฟรี ๗ วัน"` → kind UPSELL, `coins?.price == "฿29"`, `premiumTrial == "ทดลองฟรี ๗ วัน"`
  - `coinsBoughtCasts`: UPSELL, `purchaseAnswer = "purchased"`, `statusAfterSync = status(coins = 10, readingsLeft = 10)`, `onBuyCoins()` → one cast request, sheet null
  - `coinsPendingStaysWithMessage`: `purchaseAnswer = "pending"` → sheet UPSELL, `message == AWAITING_PAYMENT_MESSAGE`, no cast
  - `dismissedSheetDoesNotCastAfterPurchase`: hold the purchase answer (`purchaseAnswer = null`), `onBuyCoins()`, `onSheetDismissed()`, `answerHeld()` → no cast request, sheet null
  - `scriptureOnlyCasts`: AD sheet, `onScriptureOnly()` → one cast request, sheet null
  - `sheetTurnsReadyWhenReadingsAppear`: UPSELL open, then `session.updateStatus(status(premium = true, readingsLeft = 10))` → kind READY, no cast. `onCastFromReady()` → one cast request
  - `slowRefreshFallsBackToKnownStatus`: known status free-out with ads, `statusDelayMillis = 10_000`. `onStart`, advance 1,500 ms → sheet AD. Advance 8,500 ms more → still exactly one sheet and no cast
  - `startIsIgnoredWhileDeciding`: `statusDelayMillis = 1_000`, status `readingsLeft = 1`. Call `onStart` twice, advance 1,000 ms → exactly one cast request
  - `offlineCastsStraightAway`: build a session that is never started, with `api.statusNow = null` (status calls fail, so online stays false). `onStart` → one cast request
- [ ] **Step 3: Run them and confirm they fail.** Command: `./gradlew :composeApp:testAndroidHostTest --tests '*HomeViewModelPreCastTest*'`. Expected: compilation fails on the missing members.
- [ ] **Step 4: Implement the members above in `HomeViewModel`.** Decisions the tests don't pin:
  - `onStart`:
    - Return at once if `deciding` or the sheet is open.
    - Otherwise run `withTimeoutOrNull(1_500) { session.refreshStatus() }`, then pick `preCastOffer(session.status.value, session.online.value)`.
    - On `NONE`, emit a `CastRequest`. On `AD` or `UPSELL`, open the sheet and log `precast_sheet_shown` (`variant` = `ad` or `upsell`).
    - UPSELL also logs `quota_upsell_shown` with `placement = precast`, then loads products the same way `DivinationViewModel.onQuotaUpsellShown` does.
  - Keep the `CastRequest` from `onStart` in a private field and reuse it for every later cast from that sheet.
  - `busy` is set during `onWatchAd` and `onBuyCoins`. While busy, all actions except `onSheetDismissed` are ignored.
  - Messages and kind changes, all copied verbatim from spec section 4:
    - `onWatchAd` maps `AdResult` to the table: `Error` → `"ยังไม่มีโฆษณาพร้อมแสดง โปรดลองใหม่อีกครู่ หรือเสี่ยงทายอ่านตัวบทไปก่อน"`, and `LimitReached` → kind UPSELL (loading products).
    - `onBuyCoins` maps `PurchaseResult`: `Pending` → `PENDING_MESSAGE`, `AwaitingPayment` → `AWAITING_PAYMENT_MESSAGE`, `Error` → `"การชำระเงินไม่สำเร็จ โปรดลองใหม่อีกครั้ง"`, `Cancelled` → no message.
  - Before emitting the cast request after a successful action, check that the sheet is still open. This covers Review Focus 2.
  - In `init`, collect `session.status`. When the sheet is open, not busy, and `readingsLeft > 0`, set kind READY.
  - Analytics, all `session.log` calls:
    - `precast_choice`: `option` = `ad`, `scripture`, `premium`, `coins` or `dismiss`, from the matching action.
    - `quota_upsell_tap` with `option` and `placement = precast`.
    - `quota_upsell_purchase` with `option = coins, placement = precast` on coin `Success`.
- [ ] **Step 5: Run the tests and confirm they pass.** Same command (all 14), then the full suite `./gradlew :composeApp:testAndroidHostTest`. Expected: no failures.
- [ ] **Step 6: Commit.** Message: `Decide before casting whether to offer the ad or a pack`

### Task 4: Placement on the after-cast upsell events

**Files:**
- Modify: `frontend/composeApp/src/commonMain/kotlin/com/iching/ai/feature/divination/DivinationViewModel.kt` (the five `quota_upsell_*` log calls)

- [ ] **Step 1: Add `"placement" to "reading"`** to the params of each `quota_upsell_shown`, `quota_upsell_tap` and `quota_upsell_purchase` call. `quota_upsell_shown` currently has no params, so give it `mapOf("placement" to "reading")`.
- [ ] **Step 2: Run the suite.** `./gradlew :composeApp:testAndroidHostTest`. Expected: all pass, and `QuotaUpsellTest` is unaffected.
- [ ] **Step 3: Commit.** Message: `Tag after-cast upsell events with their placement`

### Task 5: Sheet UI and wiring

**Files:**
- Create: `frontend/composeApp/src/commonMain/kotlin/com/iching/ai/feature/home/PreCastSheet.kt`
- Modify: `frontend/composeApp/src/commonMain/kotlin/com/iching/ai/feature/home/HomeScreen.kt`
- Modify: `frontend/composeApp/src/commonMain/kotlin/com/iching/ai/App.kt` (the `screen<HomeRoute>` block)

**Interfaces:**
- Consumes: everything Task 3 produces.
- Produces: `@Composable fun PreCastSheet(ui: PreCastSheetUi, onWatchAd: () -> Unit, onBuyCoins: () -> Unit, onScriptureOnly: () -> Unit, onPremium: () -> Unit, onCastFromReady: () -> Unit, onDismiss: () -> Unit)`. `HomeScreen` gains the parameter `onOpenPaywall: () -> Unit`.

- [ ] **Step 1: Build `PreCastSheet`** as a Material3 `ModalBottomSheet` with `containerColor = ZenColors.PaperCream`.
  - Each kind's layout and copy follow spec 3.1, 3.2 and 3.3 exactly.
  - Show the question with `maxLines = 2`, or `"อธิษฐานในใจ"` when it is blank.
  - Primary and secondary buttons use `ZenPrimaryButton` and `ZenSecondaryButton`. The UPSELL scripture-only action and the AD Premium link are `TextButton`s.
  - When busy, the primary (AD) or the coins button (UPSELL) reads `"กำลังเปิดโฆษณา..."` or `"กำลังดำเนินการ..."`, and every other button is disabled.
  - Show `ui.message` as a `bodySmall` text in `CinnabarRed` under the buttons.
  - While busy, don't dismiss the sheet from the swipe gesture: use `confirmValueChange = { !ui.busy || it != SheetValue.Hidden }`.
- [ ] **Step 2: Wire it into `HomeScreen`.**
  - The start button calls `viewModel.onStart(question.trim(), QuestionCategory.fromWire(category))`, with `enabled = !deciding`.
  - Collect `viewModel.castRequests` in a `LaunchedEffect(viewModel)` and call `onCast(it.question, it.category)` for each.
  - When `sheet` is not null, show `PreCastSheet`. Its `onPremium` calls `viewModel.onPremium()` and then `onOpenPaywall()`.
- [ ] **Step 3: Wire it into `App.kt`.** Pass `onOpenPaywall = { nav.navigate(PaywallRoute) }` to `HomeScreen`.
- [ ] **Step 4: Compile both platforms.** Run `./gradlew :composeApp:compileAndroidMain :composeApp:compileKotlinIosSimulatorArm64 :composeApp:testAndroidHostTest`. Expected: BUILD SUCCESSFUL.
- [ ] **Step 5: Commit.** Message: `Show the pre-cast sheet on home`

### Task 6: Device verification

**Files:** none. Use an Android debug build with an AdMob test ad unit and a Play Billing license-test account.

- [ ] **Step 1: Run the five on-device checks** from spec section 8, in order, and note pass or fail for each.
- [ ] **Step 2: Check the copy against the spec.** Check AD, UPSELL and READY against spec 3.1–3.3 by eye, at the default font size and at the largest system font size (no clipped buttons).
- [ ] **Step 3: Check analytics.** In Firebase DebugView, `precast_sheet_shown`, `precast_choice` and `quota_upsell_*` arrive with `placement = precast`.
- [ ] **Step 4: Record the results** in the PR description, or report the failures back before merging.
