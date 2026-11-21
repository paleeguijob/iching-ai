# Poster Popup Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the home promo card with one server-driven poster: an artwork popup on home and an optional small card at the end of the daily hexagram page.

**Architecture:** `config/runtime.poster` (Firestore) is parsed and filtered by the backend into `/status.poster`. The app maps it to `AccountStatus.poster`, builds the image URL from `WEB_BASE_URL`, and decides with two pure functions: `posterPopup(...)` for home and `posterCard(...)` for the daily page. Both view models hold that state and remember ids in DataStore.

**Tech Stack:** TypeScript Cloud Functions (`node --test`), Kotlin Multiplatform + Compose, Koin, DataStore, Coil 3 (new).

**Spec:** `docs/superpowers/specs/2026-11-18-poster-popup-design.md`

## Global Constraints

- The `promo` shape and the `promo_*` names are replaced everywhere: server config, `/status`, DTO, domain, settings key, analytics, RELEASE.md. Nothing has shipped, so no migration is needed.
- Poster fields and limits:
  - `id`: non-empty, no whitespace.
  - `alt` ≤ 120.
  - `card.label` ≤ 20, `card.title` ≤ 40, `card.cta` ≤ 12.
  - `audience` ∈ {`free`, `all`}.
  - `link` ∈ {`paywall`, `daily`, `year`, `library`, `journal`} or absent.
  - `card` without `link` is invalid.
- `image` is a bare file name matching `^[A-Za-z0-9][A-Za-z0-9._-]*\.(webp|png|jpg|jpeg)$`. Ruling vs the spec's "path under `content/posters/`": no slashes, so `..`, `/`-prefixed and `http` values are all rejected.
- Image URL: `{WEB_BASE_URL trimmed of '/'}/content/posters/{image}`.
- Analytics events: `poster_shown`, `poster_tapped`, `poster_closed`, with params `poster_id` and `surface` = `popup` | `card`.
- Remembered id lists keep the newest 20.
- Day numbers use the existing `bangkokEpochDay(millis)`.
- Thai UI copy:
  - popup × contentDescription: "ปิดประกาศนี้"
  - card × contentDescription: "ปิดโปรนี้" (kept)

## Review Focus

1. A poster closed or seen in an earlier launch must not reappear while settings are still loading. Pinned in Task 4: `seenPosterNeverPopsWhileSettingsLoad`.
2. Once a popup shows, it is marked seen. It must stay on screen until the seeker closes or taps it, and must not vanish the moment `seenPosterIds` updates. Pinned in Task 4: `popupStaysUntilClosedAfterSeenIsSaved`.
3. An image that fails to load must never show an empty frame, and coming back to home must retry it. Pinned in Task 4: `failedImageNeverPopsThenRetries`.
4. A `link` this app version does not know means no poster at all, neither popup nor card. Pinned in Task 2: `statusWithUnknownLinkHasNoPoster`.
5. A Premium seeker never gets an `audience=free` poster, even from a stale cached status. Pinned in Task 3 `premiumHidesFreeCard` and Task 4 `premiumNeverGetsFreePopup`.

---

### Task 1: Backend — poster config and `/status.poster`

**Files:**
- Create: `backend/functions/src/core/poster.ts`
- Delete: `backend/functions/src/core/promo.ts`
- Modify: `backend/functions/src/core/quota.ts`, which has the `promo` field, the mergeConfig branch, `configWarnings`, `QuotaStatus.promo` and `status()`
- Test: `backend/functions/test/core.test.ts`, replacing `describe("promo")` with `describe("poster")`

**Interfaces:**
- Produces:
  - `parsePoster(raw: unknown): Poster | null`
  - `posterFor(p: Poster | null, premium: boolean, now: number): StatusPoster | null`
  - `StatusPoster = { id, image, alt, audience: "free"|"all", link: string|null, card: {label,title,cta,showEndDate}|null, endsAt: number }`
  - `RuntimeConfig.poster`, `QuotaStatus.poster`
  - `configWarnings(raw)` returns `["poster_invalid"]`

- [ ] **Step 1: Write failing tests in `describe("poster")`**
  - **Parsing:**
    - A full valid object parses, with `card` and `link`.
    - A minimal valid object (no `link`, no `card`) parses with `link` and `card` both null.
    - Each of these returns null:
      - missing or empty `id`, or an `id` containing a space;
      - `image` set to `"https://x/a.webp"`, `"../a.webp"`, `"posters/a.webp"` or `"a.gif"`;
      - `alt` of 121 chars;
      - `audience: "vip"`;
      - `link: "shop"`;
      - `card` without `link`;
      - `card.title` of 41 chars;
      - `endsAt <= startsAt`;
      - `startsAt` that is not a string.
  - **`posterFor`:**
    - Returns null when the poster is disabled, before `startsAt`, or at or after `endsAt`.
    - Returns null for `audience:"free"` with `premium=true`.
    - Returns the poster for `audience:"all"` with `premium=true`.
    - Returns the poster for `audience:"free"` with `premium=false`.
  - **`status()`:** includes `poster` from config.
  - **`configWarnings`:**
    - Returns `["poster_invalid"]` for a malformed poster.
    - Returns `[]` when the poster is absent.

- [ ] **Step 2:** `cd backend/functions && npm test`. Expected: FAIL (`parsePoster is not a function`).
- [ ] **Step 3: Implement `poster.ts` and update `quota.ts`.**
  - Follow `promo.ts`, then delete `promo.ts`.
  - `quota.ts`:
    - `promo` → `poster` in `RuntimeConfig` (default `null`), in the mergeConfig key branch and in `QuotaStatus`;
    - `status()` sets `poster: posterFor(cfg.poster, premium, now)`.
  - `index.ts` needs no change beyond what `configWarnings` already does.
- [ ] **Step 4:** `npm test && npm run typecheck:core`. Expected: all pass, no type errors.
- [ ] **Step 5: Commit** "Send one time-boxed poster in /status in place of the promo".

### Task 2: App data — poster in status and settings

**Files:**
- Modify:
  - `core/domain/.../model/Account.kt`: `PromoStatus` → `PosterStatus` and `PosterCardText`, plus `enum class PosterLink`. Also move the CampaignStatus KDoc back above CampaignStatus (deferred minor).
  - `core/domain/.../model/Hexagram.kt`: settings fields and helpers.
  - `core/data/.../api/HttpOracleApi.kt`: DTO and mapping. Add constructor param `webBaseUrl: String` after `baseUrl`.
  - `core/data/.../di/DataModule.kt`: pass `BuildConfig.WEB_BASE_URL`.
  - `core/data/.../settings/DataStoreSettingsRepository.kt`: keys.
  - `composeApp/.../feature/home/PromoCard.kt` and `HomeViewModel.kt`: compile against `status.poster` only. The card moves in Task 3.
- Test:
  - `core/domain/src/commonTest/.../DismissedPromosTest.kt` → rename to `PosterIdsTest.kt`
  - `HttpOracleApiTest.kt`, whose helper passes `"https://w"` as `webBaseUrl`
  - `SettingsPersistenceTest.kt`
  - `PromoCardTest`, `HomeViewModelPromoTest`: switch fixtures to `PosterStatus`

**Interfaces:**
- Produces:
  - `enum class PosterLink { PAYWALL, DAILY, YEAR, LIBRARY, JOURNAL }` with `companion fun fromWire(s: String): PosterLink?`, matching the lowercase names.
  - `data class PosterCardText(label: String, title: String, cta: String, showEndDate: Boolean)`
  - `data class PosterStatus(id: String, imageUrl: String, alt: String, freeOnly: Boolean, link: PosterLink?, card: PosterCardText?, endsAtMillis: Long)`
  - `AccountStatus.poster: PosterStatus? = null`, replacing `promo`.
  - `AppSettings`:
    - `seenPosterIds: List<String>`
    - `dismissedPosterCardIds: List<String>`, renamed from `dismissedPromoIds`
    - `firstOpenEpochDay: Long?`
  - `const val MAX_REMEMBERED_POSTERS = 20`, replacing `MAX_DISMISSED_PROMOS`.
  - `AppSettings.withPosterSeen(id)`, `AppSettings.withPosterCardDismissed(id)`, `AppSettings.withFirstOpen(day: Long)`. `withFirstOpen` keeps an existing value.
  - DataStore keys:
    - `seen_posters` (newline-joined)
    - `dismissed_poster_cards` (newline-joined)
    - `first_open_day` (long)

- [ ] **Step 1: Write failing tests**
  - **`PosterIdsTest`:**
    - adds an id;
    - no duplicates;
    - the 21st id drops the oldest, for both lists;
    - `withFirstOpen(5)` then `withFirstOpen(9)` keeps 5.
  - **`HttpOracleApiTest`:**
    - `statusReadsPoster`: the JSON `{"premium":false,"poster":{"id":"cny","image":"cny.webp","alt":"ตรุษจีน","audience":"free","link":"paywall","card":{"label":"โปรพิเศษ","title":"ลด","cta":"ดูโปร","showEndDate":true},"endsAt":1795194000000}}` gives `PosterStatus("cny","https://w/content/posters/cny.webp","ตรุษจีน",true,PosterLink.PAYWALL,PosterCardText("โปรพิเศษ","ลด","ดูโปร",true),1795194000000)`.
    - `statusWithoutPosterIsNull`.
    - `statusWithUnknownLinkHasNoPoster`: `"link":"shop"` gives `poster == null`, and the rest of the status still decodes.
    - `statusWithPathInImageHasNoPoster`: `"image":"../x.webp"` gives null.
  - **`SettingsPersistenceTest`:** `posterIdsAndFirstOpenSurviveReload`.
- [ ] **Step 2:** `cd frontend && ./gradlew :core:domain:jvmTest :core:data:testAndroidHostTest`. Expected: FAIL to compile on the new names.
- [ ] **Step 3: Implement the domain and data changes.**
  - Make every DTO field after `id` nullable or defaulted, so a partial poster maps to null instead of failing the whole status decode. This fixes a deferred minor.
  - The mapper checks the image regex from Global Constraints.
- [ ] **Step 4: Fix composeApp callers and run the full suite.**
  - In composeApp, `promoCard()` reads `status.poster?.card`; for now it treats `freeOnly` as before.
  - Run `./gradlew :core:domain:jvmTest :core:data:testAndroidHostTest :composeApp:testAndroidHostTest`. Expected: PASS.
- [ ] **Step 5: Commit** "Read the poster from /status and remember poster ids".

### Task 3: Card on the daily page, off home

**Files:**
- Rename `composeApp/.../feature/home/PromoCard.kt` → `composeApp/.../feature/poster/PosterCard.kt`. Move `PromoCardRow` out of `HomeScreen.kt` into `feature/poster/PosterCardRow.kt`.
- Modify:
  - `feature/daily/DailyViewModel.kt`, `feature/daily/DailyScreen.kt`
  - `feature/home/HomeViewModel.kt`, `HomeScreen.kt`: remove card state, handlers and drawing
  - `di/AppModule.kt`
  - `App.kt`: add `onOpenLink` and a `PosterLink` → route mapping
- Test:
  - `PromoCardTest` → `feature/poster/PosterCardTest.kt`
  - new `feature/daily/DailyViewModelPosterTest.kt`, which takes the card tests from `HomeViewModelPromoTest`
  - delete the card tests from `HomeViewModelPromoTest`

**Interfaces:**
- Consumes: Task 2's `PosterStatus`, `PosterLink`, `withPosterCardDismissed`.
- Produces:
  - `data class PosterCard(id: String, label: String, title: String, cta: String, link: PosterLink)`
  - `fun posterCard(status: AccountStatus, dismissed: List<String>, nowMillis: Long): PosterCard?`. Returns null when:
    - there is no `card`;
    - `link == DAILY`;
    - `freeOnly && premium`;
    - `now >= endsAt`;
    - the id was dismissed.
  - `DailyViewModel` new constructor params:
    - `status: Flow<AccountStatus>`
    - `settings: Flow<AppSettings>`
    - `dismissPosterCard: suspend (String) -> Unit`
  - `DailyViewModel.posterCard: StateFlow<PosterCard?>`
  - `DailyViewModel.onPosterCardShown(id)`, `onPosterCardTapped(id)`, `onPosterCardClosed(id)`. These log with `surface=card`; shown logs once per id per view model.
  - `DailyScreen(..., onOpenLink: (PosterLink) -> Unit)`
  - In `App.kt`, `fun NavController.open(link: PosterLink)` maps:
    - PAYWALL → PaywallRoute
    - DAILY → DailyRoute
    - YEAR → YearRoute
    - LIBRARY → LibraryRoute
    - JOURNAL → JournalRoute

- [ ] **Step 1: Write failing tests**
  - **`PosterCardTest`:** carry over the existing cases, including the label with "· ถึง ๒๐ พ.ย.". Add:
    - `premiumHidesFreeCard`
    - `premiumSeesAllAudienceCard`
    - `noCardTextNoCard`
    - `dailyLinkHidesCard`
    - `cardCarriesLink`
  - **`DailyViewModelPosterTest`:**
    - `showsCardFromStatus`
    - `closeHidesAndRemembers`: `dismissedPosterCardIds == ["cny"]`, and `poster_closed` is logged with `{poster_id: cny, surface: card}`.
    - `shownIsLoggedOnce`
    - `tapIsLogged`
    - `closedCardNeverFlashesWhileSettingsLoad`: settings start as `filterNotNull` of null, as in today's home test.
- [ ] **Step 2:** `./gradlew :composeApp:testAndroidHostTest`. Expected: FAIL to compile.
- [ ] **Step 3: Implement the card, the daily view model and the screens.**
  - `DailyScreen` draws `PosterCardRow` after the "อ่านต่อในคัมภีร์" button, with a 16dp spacer.
  - Wire `AppModule` with `status = session.status`, `settings = observeSettings()` and `dismissPosterCard = { id -> updateSettings { it.withPosterCardDismissed(id) } }`.
  - Remove all card code from home.
- [ ] **Step 4:** `./gradlew :composeApp:testAndroidHostTest :androidApp:assembleDebug`. Expected: PASS.
- [ ] **Step 5: Commit** "Move the promo card to the end of the daily hexagram page".

### Task 4: Popup on home

**Files:**
- Modify:
  - `frontend/gradle/libs.versions.toml`: `coil = "3.3.0"`, `coil-compose`, `coil-network-ktor3`. If 3.3.0 does not resolve against Compose Multiplatform 1.12.2, take the newest 3.x that does and record a Ruling.
  - `composeApp/build.gradle.kts`: commonMain dependencies.
- Create:
  - `feature/poster/PosterPopup.kt`: the pure decision.
  - `feature/poster/PosterImages.kt`: `interface PosterImages { suspend fun preload(url: String): Boolean }` and `class CoilPosterImages(context: PlatformContext) : PosterImages`, using `SingletonImageLoader.get(context).execute(ImageRequest)` with success = `SuccessResult`.
  - `feature/poster/PosterPopupDialog.kt`: the composable.
- Modify: `HomeViewModel.kt`, `HomeScreen.kt`, `di/AppModule.kt`, `App.kt` (pass `onOpenLink` to home).
- Test:
  - `feature/poster/PosterPopupTest.kt`
  - `feature/home/HomeViewModelPromoTest.kt` → rename `HomeViewModelPosterTest.kt`, with a fake `PosterImages` whose result per URL is settable

**Interfaces:**
- Consumes: Task 2 (`withPosterSeen`, `withFirstOpen`, `firstOpenEpochDay`, `seenPosterIds`) and Task 3 (`NavController.open`).
- Produces:
  - `data class PosterPopup(id: String, imageUrl: String, alt: String, link: PosterLink?)`
  - `fun posterPopup(status: AccountStatus, settings: AppSettings, today: Long, nowMillis: Long, readyImages: Set<String>): PosterPopup?`. It returns non-null only when all of these hold:
    - a poster exists;
    - its id is not in `seenPosterIds`;
    - `nowMillis < endsAtMillis`;
    - not (`freeOnly && premium`);
    - `firstOpenEpochDay != null && firstOpenEpochDay < today`;
    - `settings.streak.lastEpochDay == today`;
    - `imageUrl in readyImages`.
  - `HomeViewModel` new param `images: PosterImages`.
  - `HomeViewModel.popup: StateFlow<PosterPopup?>`. It is latched: once set, it stays until `onPopupClosed()` or `onPopupTapped()`.
  - `onPopupClosed()`
  - `onPopupTapped(): PosterLink?`
  - `HomeScreen(..., onOpenLink: (PosterLink) -> Unit)`

- [ ] **Step 1: Write failing tests**
  - **`PosterPopupTest`:** one test for each condition failing on its own, plus `allConditionsShowPopup`. Names:
    - `noPoster`, `alreadySeen`, `expiredOnDevice`, `premiumNeverGetsFreePopup`, `premiumGetsAllAudiencePopup`
    - `firstOpenUnknown`, `installDay`, `notReadToday`, `imageNotReady`
  - **`HomeViewModelPosterTest`:**
    - `recordsFirstOpenOnce`
    - `popsAfterDailyReadAndImageLoads`: logs `poster_shown {surface: popup}` and saves the id in `seenPosterIds`.
    - `popupStaysUntilClosedAfterSeenIsSaved`
    - `closeLogsAndClears`
    - `tapReturnsLinkLogsAndClears`
    - `seenPosterNeverPopsWhileSettingsLoad`
    - `failedImageNeverPopsThenRetries`: preload returns false, so no popup; set it to true and call `onResumed()`, and the popup shows.
    - `seenPosterDoesNotPopInNewViewModel`
- [ ] **Step 2:** `./gradlew :composeApp:testAndroidHostTest`. Expected: FAIL to compile.
- [ ] **Step 3: Implement the decision, preloading and latch.**
  - Add Coil and `PosterImages`.
  - `HomeViewModel`:
    - On init, `updateSettings { it.withFirstOpen(today) }`.
    - Collect `session.status`. For each new `poster.imageUrl` not yet ready, launch `images.preload(url)`; on true, add the URL to a `MutableStateFlow<Set<String>>`.
    - `onResumed()` also retries the preload when the current poster's image is not ready yet. `onResumed()` does not refresh status today, so this retry is what recovers a failed load.
    - Collect `combine(session.status, observeSettings(), today, ready)` → `posterPopup(...)`. When the result is non-null and the latch is empty: set the latch, log shown, and `updateSettings { it.withPosterSeen(id) }`.
    - Never seed settings with a default.
- [ ] **Step 4: Draw the popup.** `PosterPopupDialog` is a `Dialog(usePlatformDefaultWidth = false, dismissOnClickOutside = true)`:
  - Box, scrim 60% black.
  - `AsyncImage(imageUrl, contentDescription = alt)`:
    - width = screen − 48dp;
    - `heightIn(max = 70% of screen height)`;
    - aspect 4:5, `ContentScale.Fit`;
    - 16dp corners;
    - clickable only when `link != null`.
  - A 48dp circular × above the image's top-right, contentDescription "ปิดประกาศนี้".
  - `HomeScreen` shows it when `popup != null`:
    - tap → `onPopupTapped()?.let(onOpenLink)`;
    - close/outside → `onPopupClosed()`.
- [ ] **Step 5:** `./gradlew :core:domain:jvmTest :core:data:testAndroidHostTest :composeApp:testAndroidHostTest :androidApp:assembleDebug`. Expected: PASS.
- [ ] **Step 6: Commit** "Show the poster popup on home after today's reading".

### Task 5: Release steps and device check

**Files:**
- Modify: `docs/RELEASE.md` §14.E, rewritten as "เปิดโปสเตอร์". Steps:
  1. Put the 1080×1350 WebP, PNG or JPEG (aim ≤ 500 KB) in `backend/hosting/public/content/posters/`.
  2. `firebase deploy --only hosting`.
  3. Open `{WEB_BASE_URL}/content/posters/<file>` to confirm it loads.
  4. Set `config/runtime.poster` (field table from spec §3, dates as strings, `id` without spaces).
  5. For a discount: switch the RevenueCat offering first.
  6. Check `poster_invalid` in the function logs.
  7. To end early: `enabled: false`.
- Create: `backend/hosting/public/content/posters/.gitkeep`

- [ ] **Step 1: Write the RELEASE.md section and create the folder.**
- [ ] **Step 2: Device check with a throwaway build (never committed).**
  - Build:
    - hardcode a `PosterStatus` whose `imageUrl` is served by `python3 -m http.server` from the scratchpad via `adb reverse tcp:8000 tcp:8000`, allowing cleartext only in the throwaway;
    - set `firstOpenEpochDay` to yesterday.
  - On the emulator `emulator-5554` and the small Samsung SM-A057F, check that:
    - no popup before today's reading;
    - after reading and going back to home, the popup shows with nothing cut off;
    - × closes it, and it never returns after a relaunch;
    - tapping the image opens the paywall;
    - the card shows under "อ่านต่อในคัมภีร์" and is fully visible without scrolling on the A05 for hexagram 47;
    - the card's × hides it.
  - Afterwards: `git checkout` the throwaway files and reinstall the real APK.
- [ ] **Step 3: Commit** "Add the poster launch checklist".
