# App Rating & Feedback Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ask satisfied seekers for a store rating at a calm moment, and let anyone send feedback from Settings into Firestore.

**Architecture:** A pure `shouldRequestReview(...)` decides when Home asks the native review API (same shape as `posterPopup`). A new `NativeReview` bridge wraps Play In-App Review / `SKStoreReviewController`. Feedback goes through a new `POST /feedback` route on the `api` Cloud Function, stored in `feedback/{autoId}`.

**Tech Stack:** Kotlin Multiplatform + Compose (frontend), DataStore prefs, Ktor client; TypeScript Cloud Functions + Firestore (backend); `com.google.android.play:review-ktx`; StoreKit on iOS.

**Spec:** `docs/superpowers/specs/2026-11-20-app-rating-feedback-design.md`

## Global Constraints

- Never ask the seeker's opinion before the review prompt, never route by sentiment, never reward a rating (Play review-gating policy).
- Review prompt thresholds: completed readings ≥ 3 · `today - firstOpenEpochDay ≥ 2` · last request null or ≥ 30 days ago · no poster popup this app run.
- Record `lastReviewRequestEpochDay` the moment the API is called (the app can't know if it showed).
- "ให้คะแนนแอป" never touches `lastReviewRequestEpochDay`; hidden on iOS until an App Store id exists.
- Feedback `message`: trimmed, empty → 400, cut at 1,000 chars · 5 per uid per Bangkok day, 6th → 429 `feedback_limit`.
- Stored fields: `uid, message, appVersion, platform, premium, createdAt`.
- Account merge moves feedback to the new uid; account delete removes it.
- Analytics events: `review_prompt_requested`, `rate_app_tap`, `feedback_sent`, `feedback_failed`.
- Copy (Thai, fixed here as the builder's call from spec §7):
  - Settings rows: `ให้คะแนนแอป` · `ส่งความเห็น`
  - Sheet title `ส่งความเห็นถึงเรา` · placeholder `ชอบตรงไหน อยากให้ปรับอะไร เล่าให้เราฟังได้เลย` · button `ส่ง`
  - Success `ขอบคุณสำหรับความเห็น` · failure `ส่งไม่สำเร็จ โปรดตรวจสอบอินเทอร์เน็ตแล้วลองใหม่` · limit `วันนี้ส่งความเห็นครบแล้ว ลองใหม่พรุ่งนี้นะ`

## Review Focus

1. **App opened cold with thresholds already met** — must not prompt; only a return to Home *after a completed reading this run* may. (Test in Task 5.)
2. **Reading ends Limited / Safety / Failed** — must not count as completed. (Test in Task 4.)
3. **Feedback send fails offline** — typed text stays in the sheet; sheet stays open. (Test in Task 6.)
4. **Whitespace-only feedback** — send button disabled in app; server still 400s. (Tests in Tasks 1 and 6.)
5. **Old install upgrading** — missing prefs read as `completedReadings = 0`, `lastReviewRequestEpochDay = null`. (Test in Task 2.)

---

### Task 1: Backend `POST /feedback`

**Files:**
- Modify: `backend/functions/src/core/api.ts` (`Store` interface ~line 121; route block next to `POST /report` ~line 636)
- Modify: `backend/functions/src/store.ts` (header comment collection list; `addReport` neighbour; `mergeInto` ~line 227; `deleteUser` ~line 233)
- Test: `backend/functions/test/api.test.ts`, `backend/functions/test/store.emulator.test.ts`

**Interfaces:**
- Produces (HTTP): `POST /feedback` body `{ message: string, appVersion?: string, platform?: string }` → `200 {ok:true}` · `400 bad_request` · `429 feedback_limit`
- Produces (Store): `addFeedback(f: { uid: string; message: string; appVersion: string; platform: string; premium: boolean; createdAt: number }): Promise<void>` and `countFeedbackSince(uid: string, sinceMillis: number): Promise<number>`

- [ ] **Step 1: Write failing tests in `api.test.ts`** (fake store gets `feedback: unknown[]` plus the two methods)
  - `it("stores feedback with the app version and premium flag")` → status 200, `store.feedback[0]` matches `{ uid, message: "ชอบมาก", appVersion: "1.4.0", platform: "android", premium: false }`
  - `it("rejects empty or whitespace-only feedback")` → `"   "` → 400, nothing stored
  - `it("cuts feedback at 1000 characters")` → 1,500-char message stored with length 1000
  - `it("refuses the sixth feedback of the day")` → five 200s then 429 with error `feedback_limit`
- [ ] **Step 2: Run** `cd backend/functions && npm test` — Expected: the four new tests FAIL.
- [ ] **Step 3: Implement** the route. `message` = `typeof b.message === "string" ? b.message.trim().slice(0, 1000) : ""`; `appVersion`/`platform` sliced to 20 chars, default `""`. "Today" starts at the Bangkok day boundary — reuse the existing Bangkok-day helper the quota code uses. `premium` comes from the user's current status the route already reads for other calls.
- [ ] **Step 4: Run** `npm test` — Expected: PASS.
- [ ] **Step 5: Firestore store + emulator tests** — implement `addFeedback` (`db.collection("feedback").add`), `countFeedbackSince` (`where uid ==`, `where createdAt >=`, `.count()`); in `mergeInto` move `feedback` docs like `reports`; in `deleteUser` delete them in the same batch. Add to `store.emulator.test.ts`: `it("moves feedback on merge and removes it on delete")`. If the composite query needs an index, add it to `backend/firestore.indexes.json`.
- [ ] **Step 6: Run** `npm run test:emulator && npm run typecheck:core` — Expected: PASS.
- [ ] **Step 7: Commit** `Accept in-app feedback on the api function`

### Task 2: Settings fields and the review decision

**Files:**
- Modify: `frontend/core/domain/src/commonMain/kotlin/com/iching/ai/domain/model/Hexagram.kt` (`AppSettings`)
- Modify: `frontend/core/data/src/commonMain/kotlin/com/iching/ai/data/settings/DataStoreSettingsRepository.kt`
- Create: `frontend/composeApp/src/commonMain/kotlin/com/iching/ai/feature/review/ReviewPrompt.kt`
- Test: `frontend/composeApp/src/commonTest/kotlin/com/iching/ai/feature/review/ReviewPromptTest.kt`, `frontend/core/data/src/androidHostTest/kotlin/com/iching/ai/data/SettingsPersistenceTest.kt`

**Interfaces:**
- Produces: `AppSettings.completedReadings: Int = 0`, `AppSettings.lastReviewRequestEpochDay: Long? = null`
- Produces: `fun AppSettings.withReadingCompleted(): AppSettings`, `fun AppSettings.withReviewRequested(day: Long): AppSettings` (in `Hexagram.kt`, next to `withFirstOpen`)
- Produces: `fun shouldRequestReview(settings: AppSettings, today: Long, posterShownThisRun: Boolean): Boolean` and constants `REVIEW_MIN_READINGS = 3`, `REVIEW_MIN_DAYS_INSTALLED = 2L`, `REVIEW_COOLDOWN_DAYS = 30L` in `ReviewPrompt.kt`

- [ ] **Step 1: Write `ReviewPromptTest`** with a base `AppSettings(completedReadings = 3, firstOpenEpochDay = 100)` and `today = 102`:
  - `asksOnceThresholdsAreMet` → true
  - `waitsForThreeReadings` (`completedReadings = 2`) → false
  - `waitsTwoDaysAfterInstall` (`today = 101`) → false · `firstOpenEpochDay = null` → false
  - `waitsThirtyDaysBetweenAsks` (`last = 80`, today 109) → false; (`last = 72`, today 102) → true
  - `neverAsksAfterAPosterThisRun` → false
- [ ] **Step 2: Run** `./gradlew :composeApp:testAndroidHostTest --tests '*ReviewPromptTest*'` — Expected: FAIL (unresolved).
- [ ] **Step 3: Implement** the fields, the two `with…` helpers, and `shouldRequestReview` (KDoc states the policy reason: no opinion question beforehand).
- [ ] **Step 4: Persist** with keys `intPreferencesKey("completed_readings")`, `longPreferencesKey("last_review_request_day")`, following the `FIRST_OPEN_DAY` pattern. Add to `SettingsPersistenceTest`: round-trip both fields, and an upgrade case where neither key exists → `0` / `null`.
- [ ] **Step 5: Run** `./gradlew :composeApp:testAndroidHostTest --tests '*ReviewPromptTest*' :core:data:testAndroidHostTest` — Expected: PASS.
- [ ] **Step 6: Commit** `Decide when to ask for a store rating`

### Task 3: Feedback on the API client

**Files:**
- Modify: `frontend/core/domain/src/commonMain/kotlin/com/iching/ai/domain/repository/Repositories.kt` (`OracleApi`)
- Modify: `frontend/core/data/src/commonMain/kotlin/com/iching/ai/data/api/HttpOracleApi.kt` (next to `report`, line ~275)
- Modify: every test fake implementing `OracleApi` (add a no-op/recording override)
- Test: `frontend/core/data/src/commonTest/kotlin/com/iching/ai/data/HttpOracleApiTest.kt`

**Interfaces:**
- Produces: `suspend fun OracleApi.sendFeedback(message: String, appVersion: String, platform: String)`; throws the client's existing HTTP error type, so a 429 is distinguishable by its error code `feedback_limit`.

- [ ] **Step 1: Write test** `sendFeedbackPostsMessageVersionAndPlatform` → request is `POST /feedback` with JSON `{"message":"ชอบมาก","appVersion":"1.4.0","platform":"android"}`; and `sendFeedbackSurfacesTheDailyLimit` → 429 body `{"error":"feedback_limit"}` throws with that code.
- [ ] **Step 2: Run** `./gradlew :core:data:allTests --tests '*HttpOracleApiTest*'` — Expected: FAIL.
- [ ] **Step 3: Implement** with `call(HttpMethod.Post, "/feedback", buildJsonObject { … })`, same as `report`.
- [ ] **Step 4: Run** the same command, then `./gradlew :composeApp:testAndroidHostTest` (fakes compile) — Expected: PASS.
- [ ] **Step 5: Commit** `Send feedback from the app client`

### Task 4: `NativeReview` bridge and counting completed readings

**Files:**
- Modify: `frontend/composeApp/src/commonMain/kotlin/com/iching/ai/platform/NativeBridges.kt`
- Create: `frontend/androidApp/src/main/kotlin/com/iching/ai/platform/AndroidReview.kt`
- Modify: `frontend/androidApp/src/main/kotlin/com/iching/ai/IChingApplication.kt` (pass the bridge), `frontend/androidApp/build.gradle.kts`, `frontend/gradle/libs.versions.toml` (`play-review = "2.0.2"`, `play-review-ktx`)
- Create: `frontend/iosApp/iosApp/Bridges/IosReview.swift`; Modify: `frontend/iosApp/iosApp/iOSApp.swift`
- Modify: `frontend/composeApp/src/commonMain/kotlin/com/iching/ai/session/AppSession.kt`
- Modify: `frontend/composeApp/src/commonMain/kotlin/com/iching/ai/feature/divination/DivinationViewModel.kt` (~line 367), `frontend/composeApp/src/commonMain/kotlin/com/iching/ai/feature/siamsi/SiamSiViewModel.kt` (~line 318)
- Test: `frontend/composeApp/src/commonTest/kotlin/com/iching/ai/session/AppSessionReviewTest.kt`, plus one case each in `DivinationViewModelTest.kt` and `SiamSiViewModelTest.kt`

**Interfaces:**
- Produces:
  ```kotlin
  interface NativeReview {
      /** Asks the store's own rating sheet; value = "ok". The store may decide not to show it. */
      fun requestReview(callback: ResultCallback)
      /** Store page for the rate-app row; null where there is none yet (iOS until the App Store id exists). */
      fun storeUrl(): String?
  }
  object NoReview : NativeReview
  ```
  `NativeBridges(..., notifications, review: NativeReview = NoReview)` — default keeps existing test constructors compiling.
- Produces on `AppSession`: `suspend fun onReadingCompleted()` (bumps `completedReadings`, sets an in-memory one-shot flag), `fun takeReadingFinished(): Boolean` (returns and clears that flag), `suspend fun requestReview()` (calls the bridge; failure is logged via `recordError`, never thrown), `val storeUrl: String?`, `suspend fun sendFeedback(message: String)` (delegates to `api.sendFeedback` with `bridges.auth.appVersion()` and the platform name).

- [ ] **Step 1: Write tests**
  - `AppSessionReviewTest.readingCompletedCountsAndArmsTheFlag` → `completedReadings` 0→1, `takeReadingFinished()` true then false
  - `AppSessionReviewTest.reviewFailureIsSwallowed` → bridge returns error; no throw
  - `DivinationViewModelTest`: `doneCountsAReading` and `limitedDoesNotCountAReading`
  - `SiamSiViewModelTest`: `doneCountsAReading` and `failedDoesNotCountAReading`
- [ ] **Step 2: Run** `./gradlew :composeApp:testAndroidHostTest` — Expected: new tests FAIL.
- [ ] **Step 3: Implement** the interface, `AppSession` members, and call `session.onReadingCompleted()` inside both `DivinationEvent.Done` branches only.
- [ ] **Step 4: Android bridge** — `AndroidReview(context, activities: ActivityTracker)`: `ReviewManagerFactory.create(context)`, `requestReviewFlow()` then `launchReviewFlow(activities.current, info)`; no foreground activity → error callback. `storeUrl()` = `"https://play.google.com/store/apps/details?id=${context.packageName}"`.
- [ ] **Step 5: iOS bridge** — `IosReview`: `AppStore.requestReview(in: activeWindowScene)` (iOS 16+; else `SKStoreReviewController.requestReview(in:)`), callback `"ok"`; `storeUrl()` returns `nil`.
- [ ] **Step 6: Run** `./gradlew :composeApp:testAndroidHostTest :androidApp:assembleDebug` — Expected: PASS / BUILD SUCCESSFUL. Build the iOS app once if Xcode is available; note in the commit message if it wasn't.
- [ ] **Step 7: Commit** `Bridge the store rating sheet and count finished readings`

### Task 5: Ask from Home after a reading

**Files:**
- Modify: `frontend/composeApp/src/commonMain/kotlin/com/iching/ai/feature/home/HomeViewModel.kt` (`onResumed`, `onPopupDisplayed`)
- Test: `frontend/composeApp/src/commonTest/kotlin/com/iching/ai/feature/home/HomeViewModelReviewTest.kt` (build it like `HomeViewModelPosterTest`)

**Interfaces:**
- Consumes: `shouldRequestReview`, `withReviewRequested` (Task 2); `AppSession.takeReadingFinished()`, `requestReview()`, `log()` (Task 4).

- [ ] **Step 1: Write tests** (settings meet thresholds unless stated; fake `NativeReview` records calls)
  - `asksAfterReturningFromAFinishedReading` → one `requestReview`, `lastReviewRequestEpochDay == today`, event `review_prompt_requested` logged
  - `doesNotAskOnColdLaunch` → `onResumed()` without a finished reading → no call (Review Focus 1)
  - `doesNotAskWhenAPosterShowedThisRun` → `onPopupDisplayed(id)` earlier → no call
  - `asksAtMostOncePerCooldown` → two finished readings back to back → one call
- [ ] **Step 2: Run** `./gradlew :composeApp:testAndroidHostTest --tests '*HomeViewModelReviewTest*'` — Expected: FAIL.
- [ ] **Step 3: Implement** — a `posterShownThisRun` flag set in `onPopupDisplayed`; in `onResumed`, if `session.takeReadingFinished()` and `shouldRequestReview(latestSettings, today, posterShownThisRun || _popup.value != null)` then `updateSettings { it.withReviewRequested(today) }`, log, `session.requestReview()`.
- [ ] **Step 4: Run** the same command plus `--tests '*HomeViewModel*'` — Expected: PASS.
- [ ] **Step 5: Commit** `Ask for a rating when the seeker comes home from a reading`

### Task 6: Settings rows, feedback sheet, release notes

**Files:**
- Modify: `frontend/composeApp/src/commonMain/kotlin/com/iching/ai/feature/settings/SettingsViewModel.kt`, `SettingsScreen.kt`
- Create: `frontend/composeApp/src/commonMain/kotlin/com/iching/ai/feature/settings/FeedbackSheet.kt`
- Modify: `docs/RELEASE.md` (Data Safety: "messages the user sends as feedback, linked to their account id"; section 9: add App Store id → `IosReview.storeUrl()`)
- Test: `frontend/composeApp/src/commonTest/kotlin/com/iching/ai/feature/settings/SettingsFeedbackTest.kt`

**Interfaces:**
- Consumes: `AppSession.sendFeedback`, `storeUrl`, `log` (Task 4).
- Produces on `SettingsViewModel`:
  ```kotlin
  data class FeedbackUi(val open: Boolean = false, val text: String = "", val sending: Boolean = false, val error: String? = null) {
      val canSend: Boolean get() = text.isNotBlank() && !sending
  }
  val feedback: StateFlow<FeedbackUi>
  fun openFeedback(); fun closeFeedback(); fun onFeedbackText(text: String)  // capped at 1000
  fun sendFeedback()
  val storeUrl: String?; fun onRateAppTapped()  // logs rate_app_tap
  ```

- [ ] **Step 1: Write tests**
  - `sendsTrimmedFeedbackAndThanks` → sheet closes, `message` == `"ขอบคุณสำหรับความเห็น"`, `feedback_sent` logged
  - `keepsTextWhenSendFails` → api throws; sheet open, text kept, error = failure copy, `feedback_failed` logged (Review Focus 3)
  - `showsTheDailyLimit` → `feedback_limit` error → limit copy
  - `blankTextCannotBeSent` → `"  "` → `canSend == false` (Review Focus 4)
  - `textIsCappedAt1000`
- [ ] **Step 2: Run** `./gradlew :composeApp:testAndroidHostTest --tests '*SettingsFeedbackTest*'` — Expected: FAIL.
- [ ] **Step 3: Implement** the view model members, then the UI: two `LinkRow`s in the card that holds the privacy/terms links (`ให้คะแนนแอป` only when `storeUrl != null`, opens via `uri.openUri`); `FeedbackSheet` as a `ModalBottomSheet` with a multi-line field, a `n/1000` counter, and the `ส่ง` button (disabled unless `canSend`, shows progress while `sending`). Success uses the screen's existing `message` snackbar.
- [ ] **Step 4: Run** `./gradlew :composeApp:testAndroidHostTest :androidApp:assembleDebug` — Expected: PASS.
- [ ] **Step 5: Check in the app** — install the debug build on an emulator: open Settings, send a feedback message, confirm it lands in `feedback/` on the emulator or dev project, and tap ให้คะแนนแอป. (The Play review sheet only appears for builds installed from Play; use internal app sharing to see it, or confirm the call via the `review_prompt_requested` log.)
- [ ] **Step 6: Update `docs/RELEASE.md`** as listed under Files.
- [ ] **Step 7: Commit** `Let seekers rate the app and send feedback from Settings`
